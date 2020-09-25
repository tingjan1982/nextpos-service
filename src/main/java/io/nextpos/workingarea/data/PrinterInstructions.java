package io.nextpos.workingarea.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PrinterInstructions {

    private List<PrinterInstruction> printerInstructions;

    @Data
    @AllArgsConstructor
    public static class PrinterInstruction {

        private WorkingArea workingArea;

        private String printInstruction;

        private int noOfPrintCopies;

        private List<String> printerIpAddresses;
    }
}
