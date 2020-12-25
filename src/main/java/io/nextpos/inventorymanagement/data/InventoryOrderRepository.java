package io.nextpos.inventorymanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryOrderRepository extends MongoRepository<InventoryOrder, String> {
}
