package io.nextpos.workingarea.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingAreaRequest {

    @NotEmpty
    private String name;

    @Positive
    private int noOfPrintCopies;

    private List<String> printerIds;
}
