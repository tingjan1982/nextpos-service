package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.Supplier;

public interface SupplierService {

    Supplier saveSupplier(Supplier supplier);

    Supplier getSupplier(String id);
}
