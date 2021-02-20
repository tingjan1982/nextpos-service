package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.WorkingArea;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingAreaRequest {

    @NotEmpty
    private String name;

    private int noOfPrintCopies = 1;

    private WorkingArea.Visibility visibility = WorkingArea.Visibility.ALL;

    private List<String> printerIds;
}
