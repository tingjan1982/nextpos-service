package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.data.OrderSetRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class OrderSetServiceImpl implements OrderSetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSetServiceImpl.class);

    private final OrderSetRepository orderSetRepository;

    private final OrderService orderService;

    private final TableLayoutService tableLayoutService;

    @Autowired
    public OrderSetServiceImpl(OrderSetRepository orderSetRepository, OrderService orderService, TableLayoutService tableLayoutService) {
        this.orderSetRepository = orderSetRepository;
        this.orderService = orderService;
        this.tableLayoutService = tableLayoutService;
    }

    @Override
    public OrderSet createOrderSet(String clientId, List<String> orderIds) {

        Assert.notNull(orderIds, "orders cannot be null");

        if (orderIds.size() < 2) {
            throw new BusinessLogicException("message.mergeOrderSize", "Merge order operation needs a minimum of 2 orders");
        }

        AtomicReference<String> tableLayoutId = new AtomicReference<>();
        final List<OrderSet.OrderSetDetails> linkedOrders = orderIds.stream()
                .map(id -> {
                    final Order order = orderService.getOrder(id);

                    if (order.isTablesEmpty() || order.getTables().size() > 1) {
                        throw new BusinessLogicException("message.invalidOrder", "Order has no table or has more than one table: " + id);
                    }

                    tableLayoutId.set(order.getOneTableInfo().getTableLayoutId());
                    final TableLayout.TableDetails tableDetails = tableLayoutService.getTableDetailsOrThrows(order.getOneTableInfo().getTableId());

                    return new OrderSet.OrderSetDetails(id, order.getOneTableInfo().getTableName(), tableDetails.getScreenPosition());
                }).collect(Collectors.toList());

        final OrderSet orderSet = new OrderSet(clientId, linkedOrders, tableLayoutId.get());

        return orderSetRepository.save(orderSet);
    }

    @Override
    public OrderSet getOrderSet(String id) {
        return orderSetRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, OrderSet.class);
        });
    }

    @Override
    public OrderSet getOrderSetByOrderId(String orderId) {
        return orderSetRepository.findByMainOrderId(orderId);
    }

    @Override
    public List<OrderSet> getInFlightOrderSets(String clientId) {

        return orderSetRepository.findAllByClientIdAndStatusIsNot(clientId, OrderSet.OrderSetStatus.COMPLETED);
    }

    @Override
    public Order mergeOrderSet(OrderSet orderSet, String orderIdToMerge) {

        final boolean orderIdInSet = orderSet.getLinkedOrders().stream()
                .anyMatch(os -> os.getOrderId().equals(orderIdToMerge));

        if (!orderIdInSet) {
            throw new BusinessLogicException("message.orderIdNotInSet", "The provided order id is not part of the order set " + orderSet.getId());
        }

        final Order mainOrder = orderService.getOrder(orderIdToMerge);
        mainOrder.markOrderSetOrder();

        orderSet.getLinkedOrders().stream()
                .filter(o -> !o.getOrderId().equals(mainOrder.getId()))
                .forEach(o -> {
                    final Order mergingOrder = orderService.getOrder(o.getOrderId());
                    mainOrder.addOrderLineItems(mergingOrder.getOrderLineItems());
                    mergingOrder.deleteAllOrderLineItems();

                    orderService.saveOrder(mergingOrder);
                });

        orderSet.setMainOrderId(orderIdToMerge);
        orderSet.setStatus(OrderSet.OrderSetStatus.MERGED);
        orderSetRepository.save(orderSet);

        return orderService.saveOrder(mainOrder);
    }

    @Override
    public void settleOrderSet(OrderSet orderSet) {

        orderSet.getLinkedOrders().stream()
                .filter(os -> !os.getOrderId().equals(orderSet.getMainOrderId()))
                .forEach(os -> {
                    LOGGER.info("Settling merged oder id {}", os.getOrderId());
                    final Order order = orderService.getOrder(os.getOrderId());
                    order.setState(Order.OrderState.SETTLED);
                    orderService.saveOrder(order);
                });
    }

    @Override
    public OrderSet completeOrderSet(OrderSet orderSet) {

        orderSet.getLinkedOrders().stream()
                .filter(os -> !os.getOrderId().equals(orderSet.getMainOrderId()))
                .forEach(os -> {
                    LOGGER.info("Deleting merged oder id {}", os.getOrderId());
                    orderService.deleteOrderByOrderId(os.getOrderId());
                });

        orderSet.setStatus(OrderSet.OrderSetStatus.COMPLETED);
        return orderSetRepository.save(orderSet);
    }

    @Override
    public void deleteOrderSet(OrderSet orderSet) {

        if (orderSet.getStatus() != OrderSet.OrderSetStatus.OPEN) {
            throw new BusinessLogicException("message.cannotDelete", "The order set is either merged or completed.");
        }

        orderSetRepository.delete(orderSet);
    }
}
