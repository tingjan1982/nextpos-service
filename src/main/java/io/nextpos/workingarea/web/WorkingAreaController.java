package io.nextpos.workingarea.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import io.nextpos.workingarea.web.model.WorkingAreaRequest;
import io.nextpos.workingarea.web.model.WorkingAreaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

    private WorkingArea fromWorkingAreaRequest(final Client client, final WorkingAreaRequest workingAreaRequest) {
        final WorkingArea workingArea = new WorkingArea(client, workingAreaRequest.getName());
        workingArea.setNoOfPrintCopies(workingAreaRequest.getNoOfPrintCopies());

        return workingArea;
    }

    private WorkingAreaResponse toWorkingAreaResponse(final WorkingArea savedWorkingArea) {
        return new WorkingAreaResponse(savedWorkingArea.getId(), savedWorkingArea.getName(), savedWorkingArea.getNoOfPrintCopies());
    }
}
