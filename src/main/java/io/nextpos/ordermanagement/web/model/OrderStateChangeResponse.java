package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderStateChangeResponse {

    private String orderId;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private Instant timestamp;

    private List<PrinterInstructionResponse> printerInstructions;


    @Data
    @AllArgsConstructor
    public static class PrinterInstructionResponse {

        private String printInstruction;

        private List<String> ipAddresses;

        private int noOfPrintCopies;
    }
}
