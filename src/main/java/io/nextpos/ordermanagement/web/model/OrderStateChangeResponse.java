package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
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

        private List<String> ipAddresses;

        private int noOfPrintCopies;

        private String printInstruction;
    }
}
