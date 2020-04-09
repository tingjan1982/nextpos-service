package io.nextpos.tablelayout.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.tablelayout.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tablelayouts")
public class TableLayoutController {

    private final TableLayoutService tableLayoutService;

    private final ShiftService shiftService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public TableLayoutController(final TableLayoutService tableLayoutService, final ShiftService shiftService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.tableLayoutService = tableLayoutService;
        this.shiftService = shiftService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public TableLayoutResponse createTableLayout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @Valid @RequestBody TableLayoutRequest tableLayoutRequest) {

        TableLayout tableLayout = fromTableLayoutRequest(client, tableLayoutRequest);
        final TableLayout savedTableLayout = tableLayoutService.saveTableLayout(tableLayout);

        return toTableLayoutResponse(savedTableLayout);
    }

    private TableLayout fromTableLayoutRequest(final Client client, final TableLayoutRequest tableLayoutRequest) {
        return new TableLayout(client, tableLayoutRequest.getLayoutName());
    }

    @GetMapping("/{id}")
    public TableLayoutResponse getTableLayout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String id) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        return toTableLayoutResponse(tableLayout);
    }

    @PostMapping("/{id}")
    public TableLayoutResponse updateTableLayout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable final String id,
                                                 @Valid @RequestBody TableLayoutRequest tableLayoutRequest) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        updateTableLayoutFromRequest(tableLayout, tableLayoutRequest);

        tableLayoutService.saveTableLayout(tableLayout);

        return toTableLayoutResponse(tableLayout);
    }

    private void updateTableLayoutFromRequest(final TableLayout tableLayout, final TableLayoutRequest tableLayoutRequest) {

        tableLayout.setLayoutName(tableLayoutRequest.getLayoutName());
    }

    @GetMapping
    public TableLayoutsResponse getTableLayouts(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<TableLayout> tableLayouts = tableLayoutService.getTableLayouts(client);

        final List<TableLayoutResponse> layoutsResponse = tableLayouts.stream()
                .map(this::toTableLayoutResponse).collect(Collectors.toList());

        return new TableLayoutsResponse(layoutsResponse);
    }

    private TableLayoutResponse toTableLayoutResponse(final TableLayout savedTableLayout) {
        final List<TableDetailsResponse> tables = savedTableLayout.getTables().stream()
                .map(TableDetailsResponse::fromTableDetails)
                .collect(Collectors.toList());

        return new TableLayoutResponse(savedTableLayout.getId(),
                savedTableLayout.getLayoutName(),
                tables.size(),
                savedTableLayout.getTotalCapacity(),
                tables);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTableLayout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable final String id) {

        shiftService.getActiveShift(client.getId()).ifPresent(s -> {
            throw new BusinessLogicException("Please close shift before deleting table layout.");
        });

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        tableLayoutService.deleteTableLayout(tableLayout);
    }

    @PostMapping("/{id}/tables")
    public TableLayoutResponse createTableDetails(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable final String id,
                                                  @Valid @RequestBody TableDetailsRequest tableDetailsRequest) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        this.addTableFromTableDetailsRequest(tableLayout, tableDetailsRequest);

        final TableLayout savedTableLayout = tableLayoutService.saveTableLayout(tableLayout);
        return toTableLayoutResponse(savedTableLayout);
    }

    private void addTableFromTableDetailsRequest(final TableLayout tableLayout, final TableDetailsRequest tableDetailsRequest) {

        final TableLayout.TableDetails tableDetails = new TableLayout.TableDetails(tableDetailsRequest.getTableName(), tableDetailsRequest.getCapacity());

        tableLayout.addTableDetails(tableDetails);
    }

    @PostMapping("/{id}/tables/{tableId}")
    public TableDetailsResponse updateTableDetails(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable final String id,
                                                   @PathVariable final String tableId,
                                                   @Valid @RequestBody UpdateTableDetailsRequest updateRequest) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        final TableLayout.TableDetails tableDetails = tableLayout.getTableDetails(tableId);

        updateTableDetailsFromRequest(tableDetails, updateRequest);

        tableLayoutService.saveTableLayout(tableLayout);

        return TableDetailsResponse.fromTableDetails(tableDetails);
    }

    private void updateTableDetailsFromRequest(final TableLayout.TableDetails tableDetails, final UpdateTableDetailsRequest updateRequest) {

        tableDetails.setTableName(updateRequest.getTableName());
        tableDetails.setCapacity(updateRequest.getCapacity());
    }

    @PostMapping("/{id}/tables/{tableId}/position")
    public TableDetailsResponse updateTableDetailsPosition(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable final String id,
                                                   @PathVariable final String tableId,
                                                   @Valid @RequestBody UpdateTablePositionRequest request) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        final TableLayout.TableDetails tableDetails = tableLayout.getTableDetails(tableId);

        tableDetails.setScreenPosition(request.toScreenPosition());

        tableLayoutService.saveTableLayout(tableLayout);

        return TableDetailsResponse.fromTableDetails(tableDetails);
    }

    @DeleteMapping("/{id}/tables/{tableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTableDetails(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @PathVariable final String id,
                                   @PathVariable final String tableId) {

        final TableLayout tableLayout = clientObjectOwnershipService.checkOwnership(client, () -> tableLayoutService.getTableLayout(id));
        tableLayout.deleteTableDetails(tableId);

        tableLayoutService.saveTableLayout(tableLayout);
    }
}
