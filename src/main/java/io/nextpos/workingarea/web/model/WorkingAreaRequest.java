package io.nextpos.workingarea.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingAreaRequest {

    @NotEmpty
    private String name;

    private int noOfPrintCopies;

    private String printerId;
}
