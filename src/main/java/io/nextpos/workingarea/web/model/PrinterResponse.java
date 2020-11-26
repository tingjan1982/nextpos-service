package io.nextpos.workingarea.web.model;

import io.nextpos.workingarea.data.Printer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class PrinterResponse {

    private String id;

    private String name;

    private String ipAddress;

    @Deprecated
    private Printer.ServiceType serviceType;

    private Set<Printer.ServiceType> serviceTypes;

    public PrinterResponse(Printer printer) {
        id = printer.getId();
        name = printer.getName();
        ipAddress = printer.getIpAddress();
        serviceType = printer.getServiceType();
        serviceTypes = printer.getServiceTypes();

    }
}
