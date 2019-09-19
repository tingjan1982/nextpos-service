package io.nextpos.workingarea.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import io.nextpos.workingarea.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class WorkingAreaController {

    private final WorkingAreaService workingAreaService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public WorkingAreaController(final WorkingAreaService workingAreaService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.workingAreaService = workingAreaService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping("/workingareas")
    public WorkingAreaResponse createWorkingArea(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @Valid @RequestBody WorkingAreaRequest workingAreaRequest) {

        WorkingArea workingArea = fromWorkingAreaRequest(client, workingAreaRequest);
        final WorkingArea savedWorkingArea = workingAreaService.saveWorkingArea(workingArea);

        return toWorkingAreaResponse(savedWorkingArea);
    }

    @GetMapping("/workingareas/{id}")
    public WorkingAreaResponse getWorkingArea(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String id) {

        final WorkingArea workingArea = clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getWorkingArea(id));
        return toWorkingAreaResponse(workingArea);
    }

    @GetMapping("/workingareas")
    public WorkingAreasResponse getWorkingAreas(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {
        List<WorkingArea> workingAreas = workingAreaService.getWorkingAreas(client);

        final List<WorkingAreaResponse> workingAreaResponses = workingAreas.stream()
                .map(this::toWorkingAreaResponse).collect(Collectors.toList());

        return new WorkingAreasResponse(workingAreaResponses);
    }

    @PostMapping("/workingareas/{id}")
    public WorkingAreaResponse updateWorkingArea(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable final String id,
                                                 @Valid @RequestBody WorkingAreaRequest workingAreaRequest) {

        final WorkingArea workingAreaToUpdate = clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getWorkingArea(id));
        updateWorkingAreaFromRequest(workingAreaToUpdate, workingAreaRequest);

        return toWorkingAreaResponse(workingAreaService.saveWorkingArea(workingAreaToUpdate));
    }

    private void updateWorkingAreaFromRequest(final WorkingArea workingArea, final WorkingAreaRequest workingAreaRequest) {

        workingArea.setName(workingAreaRequest.getName());
        workingArea.setNoOfPrintCopies(workingAreaRequest.getNoOfPrintCopies());

        if (!CollectionUtils.isEmpty(workingAreaRequest.getPrinterIds())) {
            workingAreaRequest.getPrinterIds().stream()
                    .map(id -> clientObjectOwnershipService.checkOwnership(workingArea.getClient(), () -> workingAreaService.getPrinter(id)))
                    .forEach(workingArea::addPrinter);
        }
    }

    private WorkingArea fromWorkingAreaRequest(final Client client, final WorkingAreaRequest workingAreaRequest) {
        final WorkingArea workingArea = new WorkingArea(client, workingAreaRequest.getName());
        workingArea.setNoOfPrintCopies(workingAreaRequest.getNoOfPrintCopies());

        return workingArea;
    }

    private WorkingAreaResponse toWorkingAreaResponse(final WorkingArea savedWorkingArea) {

        final List<PrinterResponse> printerResponses = toPrintersResponse(savedWorkingArea.getPrinters());

        return new WorkingAreaResponse(savedWorkingArea.getId(),
                savedWorkingArea.getName(),
                savedWorkingArea.getNoOfPrintCopies(),
                printerResponses);
    }

    @PostMapping("/printers")
    public PrinterResponse createPrinter(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @Valid @RequestBody PrinterRequest printerRequest) {

        Printer printer = fromPrinterRequest(client, printerRequest);
        final Printer savedPrinter = workingAreaService.savePrinter(printer);

        return toPrinterResponse(savedPrinter);
    }

    private Printer fromPrinterRequest(final Client client, final PrinterRequest printerRequest) {

        final Printer.ServiceType serviceType = Printer.ServiceType.valueOf(printerRequest.getServiceType());
        return new Printer(client,
                printerRequest.getName(),
                printerRequest.getIpAddress(),
                serviceType);
    }

    @GetMapping("/printers/{id}")
    public PrinterResponse getPrinter(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String id) {

        final Printer printer = clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getPrinter(id));
        return toPrinterResponse(printer);
    }

    @GetMapping("/printers")
    public PrintersResponse getPrinters(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        List<Printer> printers = workingAreaService.getPrinters(client);

        return new PrintersResponse(toPrintersResponse(printers));
    }

    private List<PrinterResponse> toPrintersResponse(final List<Printer> printers) {

        return printers.stream().map(this::toPrinterResponse).collect(Collectors.toList());
    }

    @PostMapping("/printers/{id}")
    public PrinterResponse updatePrinter(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @PathVariable final String id,
                                         @Valid @RequestBody PrinterRequest printerRequest) {

        final Printer printerToUpdate = clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getPrinter(id));
        updatePrinterFromRequest(printerToUpdate, printerRequest);

        return toPrinterResponse(workingAreaService.savePrinter(printerToUpdate));
    }

    private void updatePrinterFromRequest(final Printer printer, final PrinterRequest printerRequest) {

        printer.setName(printerRequest.getName());
        printer.setIpAddress(printerRequest.getIpAddress());

        final Printer.ServiceType serviceType = Printer.ServiceType.valueOf(printerRequest.getServiceType());
        printer.setServiceType(serviceType);
    }

    private PrinterResponse toPrinterResponse(final Printer savedPrinter) {
        return new PrinterResponse(savedPrinter.getId(), savedPrinter.getName(), savedPrinter.getIpAddress(), savedPrinter.getServiceType());
    }
}
