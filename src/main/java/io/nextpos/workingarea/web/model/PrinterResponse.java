package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.Printer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrinterResponse {

    private String id;

    private String name;

    private String ipAddress;

    private Printer.ServiceType serviceType;
}
