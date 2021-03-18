package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.web.model.InventoryOrderRequest;
import io.nextpos.inventorymanagement.web.model.InventoryOrderResponse;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/inventoryOrders")
public class InventoryOrderController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryOrderController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
            supplier = inventoryService.getSupplier(request.getSupplierId());
        }

        final InventoryOrder inventoryOrder = new InventoryOrder(client.getId(), supplier, request.getSupplierOrderId());

        if (request.getOrderDate() != null) {
            inventoryOrder.setOrderDate(request.getOrderDate());
        }

        request.getItems().forEach(li -> inventoryOrder.addInventoryOrderItem(li.getInventoryId(), li.getInventoryQuantity(), li.getUnitPrice()));

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
        updateFromRequest(inventoryOrder, request);

        //return toResponse();
        return null;
    }

    private void updateFromRequest(InventoryOrder inventoryOrder, InventoryOrderRequest request) {

        if (request.getOrderDate() != null) {
            inventoryOrder.setOrderDate(request.getOrderDate());
        }

        if (StringUtils.isNotBlank(request.getSupplierId())) {
            Supplier supplier = inventoryService.getSupplier(request.getSupplierId());
            inventoryOrder.setSupplier(supplier);
        }

        inventoryOrder.setSupplierOrderId(request.getSupplierOrderId());
        request.getItems().forEach(li -> inventoryOrder.addInventoryOrderItem(li.getInventoryId(), li.getInventoryQuantity(), li.getUnitPrice()));
    }

    private InventoryOrderResponse toResponse(InventoryOrder inventoryOrder) {
        return new InventoryOrderResponse(inventoryOrder);
    }
}
