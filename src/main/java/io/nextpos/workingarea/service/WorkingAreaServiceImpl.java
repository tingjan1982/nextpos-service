package io.nextpos.workingarea.service;

import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterRepository;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.data.WorkingAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class WorkingAreaServiceImpl implements WorkingAreaService {

    private final WorkingAreaRepository workingAreaRepository;

    private final PrinterRepository printerRepository;

    @Autowired
    public WorkingAreaServiceImpl(final WorkingAreaRepository workingAreaRepository, final PrinterRepository printerRepository) {
        this.workingAreaRepository = workingAreaRepository;
        this.printerRepository = printerRepository;
    }

    @Override
    public WorkingArea saveWorkingArea(final WorkingArea workingArea) {
        return workingAreaRepository.save(workingArea);
    }

    @Override
    public WorkingArea getWorkingArea(String id) {
        return workingAreaRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, WorkingArea.class);
        });
    }

    @Override
    public Printer savePrinter(Printer printer) {
        return printerRepository.save(printer);
    }

    @Override
    public Printer getPrinter(String id) {
        return printerRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Printer.class);
        });
    }
}
