package io.nextpos.workingarea.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SinglePrintInstruction {

    private String ipAddress;

    private List<String> ipAddresses;

    private String instruction;

    public SinglePrintInstruction(String ipAddress, String instruction) {
        this.ipAddress = ipAddress;
        this.ipAddresses = List.of(ipAddress);
        this.instruction = instruction;
    }

    public SinglePrintInstruction(List<String> ipAddresses, String instruction) {
        this.ipAddresses = ipAddresses;
        this.instruction = instruction;
    }
}
