package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.data.OrderSetRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@MongoTransaction
public class OrderSetServiceImpl implements OrderSetService {

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
                    tableLayoutId.set(order.getTableInfo().getTableLayoutId());
                    final TableLayout.TableDetails tableDetails = tableLayoutService.getTableDetailsOrThrows(order.getTableInfo().getTableId());

                    return new OrderSet.OrderSetDetails(id, order.getTableInfo().getTableName(), tableDetails.getScreenPosition());
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
    public Order mergeOrderSet(OrderSet orderSet) {

        final Order mainOrder = orderService.getOrder(orderSet.getMainOrderId());
        mainOrder.markOrderSetOrder();

        orderSet.getLinkedOrders().stream()
                .filter(o -> !o.getOrderId().equals(mainOrder.getId()))
                .forEach(o -> {
                    final Order mergingOrder = orderService.getOrder(o.getOrderId());
                    mainOrder.addOrderLineItems(mergingOrder.getOrderLineItems());
                    mergingOrder.deleteAllOrderLineItems();

                    orderService.saveOrder(mergingOrder);
                });

        orderSet.setStatus(OrderSet.OrderSetStatus.MERGED);
        orderSetRepository.save(orderSet);

        return orderService.saveOrder(mainOrder);
    }

    @Override
    public OrderSet completeOrderSet(OrderSet orderSet) {

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
