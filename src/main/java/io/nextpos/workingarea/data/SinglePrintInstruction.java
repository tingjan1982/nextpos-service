package io.nextpos.workingarea.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SinglePrintInstruction {

    private String ipAddress;

    private String instruction;
}
