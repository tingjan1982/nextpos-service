package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;

import java.util.List;

public interface WorkingAreaService {

    WorkingArea saveWorkingArea(WorkingArea workingArea);

    WorkingArea getWorkingArea(String id);

    List<WorkingArea> getWorkingAreas(Client client);

    void deleteWorkingArea(WorkingArea workingArea);

    Printer savePrinter(Printer printer);

    Printer getPrinter(String id);

    Printer getPrinterByServiceType(Client client, Printer.ServiceType serviceType);

    List<Printer> getPrinters(Client client);

    void deletePrinter(Printer printer);
}
