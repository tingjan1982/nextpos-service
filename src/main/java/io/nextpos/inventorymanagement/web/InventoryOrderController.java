package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.service.SupplierService;
import io.nextpos.inventorymanagement.web.model.InventoryOrderRequest;
import io.nextpos.inventorymanagement.web.model.InventoryOrderResponse;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/inventoryOrders")
public class InventoryOrderController {

    private final InventoryService inventoryService;

    private final SupplierService supplierService;

    @Autowired
    public InventoryOrderController(InventoryService inventoryService, SupplierService supplierService) {
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;
    }

    @PostMapping
    public InventoryOrderResponse createInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                       @Valid @RequestBody InventoryOrderRequest request) {

        InventoryOrder inventoryOrder = fromRequest(client, request);

        return toResponse(inventoryService.saveInventoryOrder(inventoryOrder));
    }

    private InventoryOrder fromRequest(Client client, InventoryOrderRequest request) {

        Supplier supplier = null;

        if (StringUtils.isNotBlank(request.getSupplierId())) {
            supplier = supplierService.getSupplier(request.getSupplierId());
        }

        final InventoryOrder inventoryOrder = new InventoryOrder(client.getId(), supplier, request.getSupplierOrderId());

        if (request.getOrderDate() != null) {
            inventoryOrder.setOrderDate(request.getOrderDate());
        }

        request.getItems().forEach(li -> inventoryOrder.addInventoryOrderItem(li.getInventoryId(), li.getSku(), li.getQuantity(), li.getUnitPrice()));

        return inventoryOrder;
    }

    @GetMapping("/{id}")
    public InventoryOrderResponse getInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @PathVariable String id) {

        return toResponse(inventoryService.getInventoryOrder(id));
    }

    @PostMapping("/{id}")
    public InventoryOrderResponse updateInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                       @PathVariable String id,
                                                       @Valid @RequestBody InventoryOrderRequest request) {

        final InventoryOrder inventoryOrder = inventoryService.getInventoryOrder(id);
        checkStatus(inventoryOrder);
        updateFromRequest(inventoryOrder, request);

        return toResponse(inventoryService.saveInventoryOrder(inventoryOrder));
    }

    private void updateFromRequest(InventoryOrder inventoryOrder, InventoryOrderRequest request) {

        if (request.getOrderDate() != null) {
            inventoryOrder.setOrderDate(request.getOrderDate());
        }

        if (StringUtils.isNotBlank(request.getSupplierId())) {
            Supplier supplier = supplierService.getSupplier(request.getSupplierId());
            inventoryOrder.setSupplier(supplier);
        }

        inventoryOrder.setSupplierOrderId(request.getSupplierOrderId());
        inventoryOrder.getInventoryOrderItems().clear();
        request.getItems().forEach(li -> inventoryOrder.addInventoryOrderItem(li.getInventoryId(), li.getSku(), li.getQuantity(), li.getUnitPrice()));
    }

    private InventoryOrderResponse toResponse(InventoryOrder inventoryOrder) {
        return new InventoryOrderResponse(inventoryOrder);
    }

    @PostMapping("/{id}/copy")
    public InventoryOrderResponse copyInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                      @PathVariable String id) {

        final InventoryOrder inventoryOrder = inventoryService.getInventoryOrder(id);
        final InventoryOrder copy = inventoryService.copyInventoryOrder(inventoryOrder);
        
        return toResponse(copy);
    }

    @PostMapping("/{id}/process")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                        @PathVariable String id) {

        final InventoryOrder inventoryOrder = inventoryService.getInventoryOrder(id);
        checkStatus(inventoryOrder);

        inventoryService.processInventoryOrder(inventoryOrder);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventoryOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                      @PathVariable String id) {

        final InventoryOrder inventoryOrder = inventoryService.getInventoryOrder(id);
        checkStatus(inventoryOrder);

        inventoryService.deleteInventoryOrder(inventoryOrder);
    }

    private void checkStatus(InventoryOrder inventoryOrder) {

        if (inventoryOrder.getStatus() == InventoryOrder.InventoryOrderStatus.PROCESSED) {
            throw new BusinessLogicException("message.alreadyProcessed", "Inventory order is already processed.");
        }
    }
}
