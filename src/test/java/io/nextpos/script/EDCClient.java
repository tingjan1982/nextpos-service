package io.nextpos.script;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Socket:
 * https://www.baeldung.com/a-guide-to-java-sockets
 *
 * Calculate LRC:
 * https://stackoverflow.com/questions/6221886/calculating-lrc-in-java
 */
@Disabled
public class EDCClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void connect() throws Exception {

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        final Message message = createCancelMessage();

        final String data = objectMapper.writeValueAsString(message);

        System.out.println(data);
        final String dataAsHex = Hex.encodeHexString(data.getBytes(), false);
        System.out.println(dataAsHex);

        final SocketClient client = new SocketClient();
        client.startConnection("192.168.2.242", 1234);
        StringBuilder content = new StringBuilder();
        content.append(new String(new byte[]{0x02}));
        content.append(data);
        content.append(new String(new byte[]{0x03}));
        content.append(computeLRC(data.getBytes()));

        System.out.println(content);

        client.sendMessage(content.toString());

        client.stopConnection();
    }

    private Message createCardMessage(String amount) {

        final Message message = new Message();
        message.setPackageName("com.cybersoft.a920");
        message.setDestinationClass("com.cybersoft.a920.activity.MainActivity");
        Message.Request request = new Message.Request();
        request.setTransType("11");
        request.setTransAmount(amount);
        message.setPosRequest(request);

        return message;
    }

    private Message createCancelMessage() {

        final Message message = new Message();
        message.setPackageName("com.cybersoft.a920");
        message.setDestinationClass("com.cybersoft.a920.activity.MainActivity");
        Message.Request request = new Message.Request();
        request.setTransType("30");
        request.setReceiptNo("131223");
        message.setPosRequest(request);

        return message;
    }

    private Message createJkoMessage(String amount) {

        final Message message = new Message();
        message.setPackageName("com.cybersoft.a920.scp");
        message.setDestinationClass("com.cybersoft.a920.scp.activity.MainActivity");
        Message.Request request = new Message.Request();
        request.setTransType("11");
        request.setTransAmount(amount);
        request.setWallet("LINEPAY_I");
        request.setFundingTradeAmount(amount);
        message.setPosRequest(request);

        return message;
    }

    @Test
    void testComputeLRC() {

        System.out.println(computeLRC("ABC".getBytes()));
    }

    private String computeLRC(byte[] bytes) {

        byte lrc = 0x00;

        for (int i = 0; i < bytes.length; i++) {
            lrc ^= bytes[i];
        }

        lrc ^= 0x03;

        //return String.format("%02X", lrc);
        return new String(new byte[]{lrc});
        //return Hex.encodeHexString(new byte[]{lrc});
    }

    @Data
    public static class Message {

        @JsonProperty("PackageName")
        private String packageName;

        @JsonProperty("DestinationClass")
        private String destinationClass;

        @JsonProperty("POS_Request")
        private Request posRequest;

        @Data
        static class Request {

            @JsonProperty("Trans_Type")
            private String transType;

            @JsonProperty("Receipt_No")
            private String receiptNo;

            @JsonProperty("Trans_Amount")
            private String transAmount;

            @JsonProperty("Wallet")
            private String wallet;

            @JsonProperty("FundingTradeAmount")
            private String fundingTradeAmount;

        }
    }

    public static class SocketClient {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public void startConnection(String ip, int port) throws Exception {

            clientSocket = new Socket(ip, port);
            clientSocket.setSoTimeout(5000);
            out = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.US_ASCII);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println(in.readLine());
        }

        /**
         * https://www.baeldung.com/convert-input-stream-to-string
         */
        public String sendMessage(String msg) throws Exception {
            out.println(msg);

            int c;
            StringBuilder textBuilder = new StringBuilder();

            try {
                while ((c = in.read()) != -1) {
                    textBuilder.append((char) c);
                    System.out.print((char) c);
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Nothing to read from socket: " + e.getMessage());
            }

            System.out.println(textBuilder);

            return textBuilder.toString();
        }

        public void stopConnection() throws Exception {

            in.close();
            out.close();
            clientSocket.close();
        }
    }
}
