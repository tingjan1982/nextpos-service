package io.nextpos.ordermanagement.boundedcontext;

import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.ordermanagement.data.Order;

import java.util.Optional;

public interface InventoryTransactionContextualService {

    Optional<InventoryTransaction> createAndProcessInventoryTransaction(Order order);
}
