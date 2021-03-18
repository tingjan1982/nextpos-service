package io.nextpos.inventorymanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InventoryRepository extends MongoRepository<Inventory, String> {

    Optional<Inventory> findByClientIdAndProductId(String clientId, String productId);
}
