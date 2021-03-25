package io.nextpos.inventorymanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InventoryOrderRepository extends MongoRepository<InventoryOrder, String> {

    List<InventoryOrder> findAllByClientIdOrderByModifiedDateDesc(String clientId);
}
