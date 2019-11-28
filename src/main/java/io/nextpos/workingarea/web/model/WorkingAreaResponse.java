package io.nextpos.workingarea.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingAreaResponse {

    private String id;

    private String name;

    private int noOfPrintCopies;

    private List<String> printerIds;

    private List<PrinterResponse> printers;
}
