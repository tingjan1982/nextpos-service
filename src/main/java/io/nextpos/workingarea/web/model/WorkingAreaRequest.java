package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.WorkingArea;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

    @NotNull
    private WorkingArea.Visibility visibility;

    private List<String> printerIds;
}
