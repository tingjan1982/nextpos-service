package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterRepository;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.data.WorkingAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@JpaTransaction
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
    public List<WorkingArea> getWorkingAreas(final Client client) {
        return workingAreaRepository.findAllByClient(client);
    }

    @Override
    public void deleteWorkingArea(final WorkingArea workingArea) {
        workingAreaRepository.delete(workingArea);
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

    @Override
    public List<Printer> getPrinters(final Client client) {
        return printerRepository.findAllByClient(client);
    }

    @Override
    public void deletePrinter(final Printer printer) {
        printerRepository.delete(printer);
    }
}
