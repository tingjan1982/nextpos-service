package io.nextpos.workingarea.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingAreaResponse {

    private String id;

    private String name;

    private int noOfPrintCopies;
}
