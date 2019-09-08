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

    // todo: make this a list of printer ids and update postman to create printer and working area and link them together.
    private String printerId;
}
