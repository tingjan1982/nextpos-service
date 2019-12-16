package io.nextpos.tablelayout.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.tablelayout.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tablelayouts")
public class TableLayoutController {

    private final TableLayoutService tableLayoutService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public TableLayoutController(final TableLayoutService tableLayoutService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.tableLayoutService = tableLayoutService;
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
        return new TableLayout(client, tableLayoutRequest.getLayoutName(), tableLayoutRequest.getGridSizeX(), tableLayoutRequest.getGridSizeY());
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
        tableLayout.setGridSizeX(tableLayoutRequest.getGridSizeX());
        tableLayout.setGridSizeY(tableLayoutRequest.getGridSizeY());
    }

    @GetMapping
    public TableLayoutsResponse getTableLayouts(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<TableLayout> tableLayouts = tableLayoutService.getTableLayouts(client);

        final List<TableLayoutResponse> layoutsResponse = tableLayouts.stream()
                .map(this::toTableLayoutResponse).collect(Collectors.toList());

        return new TableLayoutsResponse(layoutsResponse);
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

        final TableLayout.TableDetails tableDetails = new TableLayout.TableDetails(tableDetailsRequest.getTableName(), tableDetailsRequest.getCoordinateX(), tableDetailsRequest.getCoordinateY());
        tableDetails.setCapacity(tableDetailsRequest.getCapacity());

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

        return new TableDetailsResponse(tableDetails.getId(),
                tableDetails.getTableName(),
                tableDetails.getXCoordinate(),
                tableDetails.getYCoordinate(),
                tableDetails.getCapacity());
    }

    private void updateTableDetailsFromRequest(final TableLayout.TableDetails tableDetails, final UpdateTableDetailsRequest updateRequest) {

        tableDetails.setTableName(updateRequest.getTableName());
        tableDetails.setCapacity(updateRequest.getCapacity());
    }

    private TableLayoutResponse toTableLayoutResponse(final TableLayout savedTableLayout) {
        final List<TableDetailsResponse> tables = savedTableLayout.getTables().stream()
                .map(t -> new TableDetailsResponse(t.getId(), t.getTableName(), t.getXCoordinate(), t.getYCoordinate(), t.getCapacity()))
                .collect(Collectors.toList());

        return new TableLayoutResponse(savedTableLayout.getId(),
                savedTableLayout.getLayoutName(),
                savedTableLayout.getGridSizeX(),
                savedTableLayout.getGridSizeY(),
                tables.size(),
                savedTableLayout.getTotalCapacity(),
                tables);
    }
}
