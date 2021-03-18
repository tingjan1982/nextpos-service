package io.nextpos.ordermanagement.boundedcontext;

import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class InventoryTransactionContextualServiceImpl implements InventoryTransactionContextualService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryTransactionContextualServiceImpl.class);

    public final InventoryService inventoryService;

    @Autowired
    public InventoryTransactionContextualServiceImpl(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public Optional<InventoryTransaction> createAndProcessInventoryTransaction(Order order) {

        final List<OrderLineItem> lineItems = order.getOrderLineItems().stream()
                .filter(li -> StringUtils.isNotBlank(li.getProductSnapshot().getSku()))
                .collect(Collectors.toList());

        if (!lineItems.isEmpty()) {
            final InventoryTransaction inventoryTransaction = new InventoryTransaction(order.getClientId(), order.getId());

            for (OrderLineItem lineItem : lineItems) {
                final String productId = lineItem.getProductSnapshot().getId();
                final String sku = lineItem.getProductSnapshot().getSku();

                inventoryService.getInventoryByProductId(order.getClientId(), productId).ifPresent(i -> {
                    inventoryTransaction.addInventoryTransactionItem(i.getId(), sku, lineItem.getQuantity());
                });
            }

            if (inventoryTransaction.hasInventoryTransactionItems()) {
                LOGGER.info("Creating inventory transaction for order {}", order.getId());
                final InventoryTransaction saved = inventoryService.saveInventoryTransaction(inventoryTransaction);
                inventoryService.processInventoryTransaction(saved);

                return Optional.of(saved);
            }
        }

        return Optional.empty();
    }
}
