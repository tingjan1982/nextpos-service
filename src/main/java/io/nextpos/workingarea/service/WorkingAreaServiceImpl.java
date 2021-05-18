package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.product.data.ProductLabelRepository;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterRepository;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.data.WorkingAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@JpaTransaction
public class WorkingAreaServiceImpl implements WorkingAreaService {

    private final WorkingAreaRepository workingAreaRepository;

    private final PrinterRepository printerRepository;

    private final ProductRepository productRepository;

    private final ProductLabelRepository productLabelRepository;

    private final ClientUserRepository clientUserRepository;

    @Autowired
    public WorkingAreaServiceImpl(final WorkingAreaRepository workingAreaRepository, final PrinterRepository printerRepository, ProductRepository productRepository, ProductLabelRepository productLabelRepository, ClientUserRepository clientUserRepository) {
        this.workingAreaRepository = workingAreaRepository;
        this.printerRepository = printerRepository;
        this.productRepository = productRepository;
        this.productLabelRepository = productLabelRepository;
        this.clientUserRepository = clientUserRepository;
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
    public List<WorkingArea> getWorkingAreas(Client client, WorkingArea.Visibility visibility) {

        if (visibility == WorkingArea.Visibility.ALL) {
            return this.getWorkingAreas(client);
        }

        return workingAreaRepository.findAllByClientAndVisibilityIn(client, List.of(WorkingArea.Visibility.ALL, visibility));
    }

    @Override
    public void deleteWorkingArea(final WorkingArea workingArea) {

        if (!checkWorkingAreaDeletable(workingArea)) {
            throw new BusinessLogicException("message.workingAreaInUse", "Working area is associated with at least product, category or user.");
        }

        workingArea.clearPrinters();

        workingAreaRepository.delete(workingArea);
    }

    private boolean checkWorkingAreaDeletable(WorkingArea workingArea) {
        return !productRepository.existsAllByWorkingArea(workingArea) &&
                !productLabelRepository.existsAllByWorkingArea(workingArea) &&
                !clientUserRepository.existsAllByWorkingAreas(workingArea);
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
    public List<Printer> getPrintersByServiceType(Client client, Printer.ServiceType serviceType) {
        return printerRepository.findByClientAndServiceTypes(client, serviceType);
    }

    @Override
    public List<Printer> getPrinters(final Client client) {
        return printerRepository.findAllByClient(client);
    }

    @Override
    public void deletePrinter(final Printer printer) {

        if (!CollectionUtils.isEmpty(printer.getWorkingAreas())) {
            throw new BusinessLogicException("message.printerHasWorkingArea", "Printer is associated with at least 1 working area");
        }

        printerRepository.delete(printer);
    }
}
