package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.*;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
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

    private final InventoryOrderRepository inventoryOrderRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository, InventoryOrderRepository inventoryOrderRepository, InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryOrderRepository = inventoryOrderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public Inventory createStock(CreateInventory createInventory) {

        final Inventory stock = Inventory.createStock(createInventory);

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
    public Inventory getInventoryByProductIdOrThrows(String clientId, String productId) {
        return inventoryRepository.findByClientIdAndProductId(clientId, productId).orElseThrow(() -> {
            throw new ObjectNotFoundException(productId, Inventory.class);
        });
    }

    @Override
    public Optional<Inventory> getInventoryByProductId(String clientId, String productId) {
        return inventoryRepository.findByClientIdAndProductId(clientId, productId);
    }

    @Override
    public void deleteInventory(Inventory inventory) {
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryOrder saveInventoryOrder(InventoryOrder inventoryOrder) {

        if (inventoryOrder.getStatus() == InventoryOrder.InventoryOrderStatus.PROCESSED) {
            throw new BusinessLogicException("message.inventoryOrderImported", "Processed inventory order cannot be changed.");
        }

        return inventoryOrderRepository.save(inventoryOrder);
    }

    @Override
    public InventoryOrder getInventoryOrder(String id) {
        return inventoryOrderRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, InventoryOrder.class);
        });
    }

    @Override
    public InventoryOrder copyInventoryOrder(InventoryOrder inventoryOrder) {
        final InventoryOrder newInventoryOrder = inventoryOrder.copy();

        return this.saveInventoryOrder(newInventoryOrder);
    }

    @Override
    public void processInventoryOrder(InventoryOrder inventoryOrder) {

        inventoryOrder.getInventoryOrderItems().forEach(i -> {
            final Inventory inventory = getInventory(i.getInventoryId());
            inventory.updateInventoryQuantity(i.getSku(), i.getQuantity());

            saveInventory(inventory);
        });

        inventoryOrder.setStatus(InventoryOrder.InventoryOrderStatus.PROCESSED);
        inventoryOrderRepository.save(inventoryOrder);
    }

    @Override
    public void deleteInventoryOrder(InventoryOrder inventoryOrder) {
        inventoryOrderRepository.delete(inventoryOrder);
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
            inventory.updateInventoryQuantity(i.getSku(), i.getQuantity().negate());

            saveInventory(inventory);
        });

        inventoryTransaction.setStatus(InventoryTransaction.InventoryTransactionStatus.PROCESSED);
        inventoryTransactionRepository.save(inventoryTransaction);
    }
}
