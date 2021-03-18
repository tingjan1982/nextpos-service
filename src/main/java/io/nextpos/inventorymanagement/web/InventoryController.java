package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.web.model.CreateInventoryRequest;
import io.nextpos.inventorymanagement.web.model.InventoryResponse;
import io.nextpos.inventorymanagement.web.model.UpdateInventoryRequest;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryResponse createInventory(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @Valid @RequestBody CreateInventoryRequest request) {

        final Inventory inventory = inventoryService.createStock(request.toCreateInventory(client.getId()));

        return toResponse(inventory);
    }

    @GetMapping
    public InventoryResponse getInventoryBySku(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @RequestParam("sku") String sku) {

        final Inventory inventory = inventoryService.getInventoryBySku(client.getId(), sku).orElseThrow(() -> {
            throw new ObjectNotFoundException(sku, Inventory.class);
        });

        return toResponse(inventory);
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventoryByProductId(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                     @PathVariable String productId) {

        final Inventory inventory = inventoryService.getInventoryByProductId(client.getId(), productId);
        return toResponse(inventory);
    }

    @PostMapping("/{productId}")
    public InventoryResponse updateInventory(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String productId,
                                             @Valid @RequestBody UpdateInventoryRequest request) {

        final Inventory inventory = inventoryService.getInventoryByProductId(client.getId(), productId);
        updateInventoryFromRequest(inventory, request);

        return toResponse(inventoryService.saveInventory(inventory));
    }

    private void updateInventoryFromRequest(Inventory inventory, UpdateInventoryRequest request) {

        inventory.setSku(request.getSku());
        inventory.setMinimumStockLevel(request.getMinimumStockLevel());
        request.getQuantities().forEach(inventory::replaceInventoryQuantity);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(inventory);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                @PathVariable String productId) {

        final Inventory inventory = inventoryService.getInventoryByProductId(client.getId(), productId);
        inventoryService.deleteInventory(inventory);
    }
}
