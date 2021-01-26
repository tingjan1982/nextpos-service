package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.WorkingArea;
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

    private WorkingArea.Visibility visibility = WorkingArea.Visibility.ALL;

    private List<String> printerIds;
}
