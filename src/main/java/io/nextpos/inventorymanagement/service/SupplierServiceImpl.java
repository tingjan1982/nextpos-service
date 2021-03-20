package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.data.SupplierRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ChainedTransaction
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Autowired
    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Override
    public Supplier getSupplier(String id) {
        return supplierRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Supplier.class);
        });
    }

}
