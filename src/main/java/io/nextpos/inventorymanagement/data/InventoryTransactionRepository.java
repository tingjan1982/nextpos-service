package io.nextpos.inventorymanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction, String> {
}
