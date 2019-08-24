package io.nextpos.workingarea.service;

import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;

public interface WorkingAreaService {

    WorkingArea saveWorkingArea(WorkingArea workingArea);

    WorkingArea getWorkingArea(String id);

    Printer savePrinter(Printer printer);

    Printer getPrinter(String id);
}
