package io.nextpos.inventorymanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.SupplierService;
import io.nextpos.inventorymanagement.web.model.SupplierRequest;
import io.nextpos.inventorymanagement.web.model.SupplierResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }
    
    @PostMapping
    public SupplierResponse createSupplier(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @Valid @RequestBody SupplierRequest request) {

        Supplier supplier = fromRequest(client, request);

        return toResponse(supplierService.saveSupplier(supplier));
    }

    private Supplier fromRequest(Client client, SupplierRequest request) {

        final Supplier supplier = new Supplier(client.getId(), request.getSupplierName());
        final Supplier.ContactInfo contactInfo = Supplier.ContactInfo.builder()
                .contactPerson(request.getContactPerson())
                .contactEmail(request.getContactEmail())
                .contactNumber(request.getContactNumber())
                .contactAddress(request.getContactAddress()).build();

        supplier.setContactInfo(contactInfo);

        return supplier;
    }

    @GetMapping("/{id}")
    public SupplierResponse getSupplier(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                        @PathVariable String id) {

        return toResponse(supplierService.getSupplier(id));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(supplier);
    }
}
