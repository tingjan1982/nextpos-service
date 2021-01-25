package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.WorkingArea;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorkingAreaResponse {

    private String id;

    private String name;

    private int noOfPrintCopies;

    private List<String> printerIds;

    private List<PrinterResponse> printers;

    private WorkingArea.Visibility visibility;
}
