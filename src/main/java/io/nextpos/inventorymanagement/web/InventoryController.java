package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.web.model.CreateInventoryRequest;
import io.nextpos.inventorymanagement.web.model.InventoryResponse;
import io.nextpos.inventorymanagement.web.model.UpdateInventoryRequest;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
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

    @GetMapping("/{productId}")
    public InventoryResponse getInventoryByProductId(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                     @PathVariable String productId) {

        final Inventory inventory = inventoryService.getInventoryByProductIdOrThrows(client.getId(), productId);
        return toResponse(inventory);
    }

    @PostMapping("/{productId}/quantities")
    public InventoryResponse addInventoryQuantity(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable String productId,
                                                  @Valid @RequestBody UpdateInventoryRequest request) {

        final Inventory inventory = inventoryService.getInventoryByProductIdOrThrows(client.getId(), productId);
        final String sku = request.getQuantity().getSku();

        if (inventory.getInventoryQuantity(sku) != null) {
            throw new ObjectAlreadyExistsException(sku, Inventory.InventoryQuantity.class);
        }

        updateInventoryFromRequest(inventory, sku, request);

        return toResponse(inventoryService.saveInventory(inventory));
    }

    @PostMapping("/{productId}/quantities/{sku}")
    public InventoryResponse updateInventory(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String productId,
                                             @PathVariable String sku,
                                             @Valid @RequestBody UpdateInventoryRequest request) {

        final Inventory inventory = inventoryService.getInventoryByProductIdOrThrows(client.getId(), productId);

        if (inventory.getInventoryQuantity(sku) == null) {
            throw new ObjectNotFoundException(sku, Inventory.InventoryQuantity.class);
        }

        updateInventoryFromRequest(inventory, sku, request);

        return toResponse(inventoryService.saveInventory(inventory));
    }

    private void updateInventoryFromRequest(Inventory inventory, String prevSku, UpdateInventoryRequest request) {

        inventory.removeInventoryQuantity(prevSku);
        inventory.replaceInventoryQuantity(request.getQuantity());
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(inventory);
    }

    @DeleteMapping("/{productId}/quantities/{sku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventoryQuantity(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                        @PathVariable String productId,
                                        @PathVariable String sku) {

        final Inventory inventory = inventoryService.getInventoryByProductIdOrThrows(client.getId(), productId);
        inventory.removeInventoryQuantity(sku);

        inventoryService.saveInventory(inventory);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                @PathVariable String productId) {

        final Inventory inventory = inventoryService.getInventoryByProductIdOrThrows(client.getId(), productId);
        inventoryService.deleteInventory(inventory);
    }
}
