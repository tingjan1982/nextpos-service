package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PrinterInstructionResponse {

    private String printInstruction;

    private List<String> ipAddresses;

    private int noOfPrintCopies;
}
