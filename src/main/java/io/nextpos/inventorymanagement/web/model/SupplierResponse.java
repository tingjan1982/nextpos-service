package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupplierResponse {

    private String id;

    private String supplierName;

    private String contactPerson;

    private String contactEmail;

    private String contactNumber;

    private String contactAddress;

    public SupplierResponse(Supplier supplier) {

        id = supplier.getId();
        supplierName = supplier.getName();
        contactPerson = supplier.getContactInfo().getContactPerson();
        contactEmail = supplier.getContactInfo().getContactEmail();
        contactNumber = supplier.getContactInfo().getContactNumber();
        contactAddress = supplier.getContactInfo().getContactAddress();
    }
}
