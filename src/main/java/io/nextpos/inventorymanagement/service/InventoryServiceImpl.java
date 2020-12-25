package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ChainedTransaction
public class InventoryServiceImpl implements InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;

    private final SupplierRepository supplierRepository;

    private final InventoryOrderRepository inventoryOrderRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, SupplierRepository supplierRepository, InventoryOrderRepository inventoryOrderRepository, InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryOrderRepository = inventoryOrderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public Inventory createStock(String clientId, String sku, Inventory.InventoryQuantity inventoryQuantity) {

        final Inventory stock = Inventory.createStock(clientId, sku);
        stock.updateInventoryQuantity(inventoryQuantity);

        return saveInventory(stock);
    }

    @Override
    public Inventory saveInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory getInventory(String id) {
        return inventoryRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Inventory.class);
        });
    }

    @Override
    public Optional<Inventory> getInventoryBySku(String clientId, String sku) {
        return inventoryRepository.findByClientIdAndSku(clientId, sku);
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

    @Override
    public InventoryOrder saveInventoryOrder(InventoryOrder inventoryOrder) {

        if (inventoryOrder.getStatus() == InventoryOrder.InventoryOrderStatus.PROCESSED) {
            throw new BusinessLogicException("message.inventoryOrderImported", "Processed inventory order cannot be changed.");
        }

        return inventoryOrderRepository.save(inventoryOrder);
    }

    @Override
    public void processInventoryOrder(InventoryOrder inventoryOrder) {

        inventoryOrder.getInventoryOrderItems().forEach(i -> {
            final Inventory inventory = getInventory(i.getInventoryId());
            inventory.updateInventoryQuantity(i.getInventoryQuantity());

            saveInventory(inventory);
        });

        inventoryOrder.setStatus(InventoryOrder.InventoryOrderStatus.PROCESSED);
        inventoryOrderRepository.save(inventoryOrder);
    }

    @Override
    public InventoryTransaction saveInventoryTransaction(InventoryTransaction inventoryTransaction) {
        return inventoryTransactionRepository.save(inventoryTransaction);
    }

    @Override
    public void processInventoryTransaction(InventoryTransaction inventoryTransaction) {

        LOGGER.info("Processing inventory transaction {}, item count: {}", inventoryTransaction.getId(), inventoryTransaction.getInventoryTransactionItems().size());

        inventoryTransaction.getInventoryTransactionItems().forEach(i -> {
            final Inventory inventory = getInventory(i.getInventoryId());
            inventory.updateInventoryQuantity(i.getInventoryQuantity());

            saveInventory(inventory);
        });

        inventoryTransaction.setStatus(InventoryTransaction.InventoryTransactionStatus.PROCESSED);
        inventoryTransactionRepository.save(inventoryTransaction);
    }
}
