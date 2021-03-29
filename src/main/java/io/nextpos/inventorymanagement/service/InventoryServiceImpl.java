package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.*;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.inventorymanagement.service.bean.InventorySku;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ChainedTransaction
public class InventoryServiceImpl implements InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductService productService;

    private final MongoTemplate mongoTemplate;

    private final InventoryRepository inventoryRepository;

    private final InventoryOrderRepository inventoryOrderRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    public InventoryServiceImpl(ProductService productService, MongoTemplate mongoTemplate, InventoryRepository inventoryRepository, InventoryOrderRepository inventoryOrderRepository, InventoryTransactionRepository inventoryTransactionRepository) {
        this.productService = productService;
        this.mongoTemplate = mongoTemplate;
        this.inventoryRepository = inventoryRepository;
        this.inventoryOrderRepository = inventoryOrderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public Inventory createStock(CreateInventory createInventory) {

        final String productName = productService.getProduct(createInventory.getProductId()).getDesignVersion().getProductName();
        final Inventory stock = Inventory.createStock(createInventory);
        stock.setProductName(productName);

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
    public List<InventorySku> searchInventorySkusByKeyword(String clientId, String keyword) {

        ProjectionOperation projections = Aggregation.project("id", "productName")
                .and(ObjectOperators.ObjectToArray.valueOfToArray("inventoryQuantities")).as("quantities");

        MatchOperation matchClient = Aggregation.match(Criteria.where("clientId").is(clientId));
        UnwindOperation flattenQuantities = Aggregation.unwind("quantities");

        ProjectionOperation output = Aggregation.project("productName")
                .and("id").as("inventoryId")
                .and("quantities.k").as("sku")
                .and("quantities.v.name").as("skuName");

        MatchOperation search = Aggregation.match(
                new Criteria().orOperator(
                        Criteria.where("productName").regex(keyword),
                        Criteria.where("quantities.k").regex(keyword))
        );

        TypedAggregation<Inventory> aggregations = Aggregation.newAggregation(Inventory.class,
                matchClient,
                projections,
                flattenQuantities,
                search,
                output
        );
        final AggregationResults<InventorySku> results = mongoTemplate.aggregate(aggregations, InventorySku.class);

        return results.getMappedResults();
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
    public List<InventoryOrder> getInventoryOrders(String clientId) {
        return inventoryOrderRepository.findAllByClientIdOrderByModifiedDateDesc(clientId);
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
