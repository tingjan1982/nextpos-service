package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.web.model.CreateInventoryRequest;
import io.nextpos.inventorymanagement.web.model.InventoryResponse;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
                                             @RequestBody CreateInventoryRequest request) {

        final Inventory inventory = inventoryService.createStock(client.getId(), request.getSku(), Inventory.InventoryQuantity.each(0));

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

    @GetMapping("/{id}")
    public InventoryResponse getInventory(@PathVariable String id) {

        final Inventory inventory = inventoryService.getInventory(id);
        return toResponse(inventory);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(inventory);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@PathVariable String id) {

        final Inventory inventory = inventoryService.getInventory(id);
        inventoryService.deleteInventory(inventory);
    }
}
