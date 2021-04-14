package io.nextpos.ordermanagement.event;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostOrderStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;

    private Order order;


    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final WorkingArea workingArea = new WorkingArea(client, "bar");
        workingAreaService.saveWorkingArea(workingArea);

        order = new Order(client.getId(), orderSettings);

        final OrderLineItem item1 = new OrderLineItem(DummyObjects.productSnapshot(), 2, orderSettings);
        item1.setWorkingAreaId(workingArea.getId());
        item1.setState(OrderLineItem.LineItemState.IN_PROCESS);

        order.addOrderLineItem(item1);
        orderService.saveOrder(order);
    }

    @Test
    void postOrderStateChange() throws Exception {

        order.setState(Order.OrderState.IN_PROCESS);
        final OrderStateChange orderStateChange = new OrderStateChange(order.getId(), client.getId());
        final OrderStateChangeBean orderStateChangeBean = new OrderStateChangeBean(orderStateChange, order);
        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();

        eventPublisher.publishEvent(new PostStateChangeEvent(this, order, orderStateChangeBean, future));

        final OrderStateChangeBean result = future.get();

        assertThat(result.getPrinterInstructions()).isNotNull();
    }

    @Test
    void postOrderStateChange_NotInProcess() throws Exception {

        order.setState(Order.OrderState.DELIVERED);
        final OrderStateChange orderStateChange = new OrderStateChange(order.getId(), client.getId());
        final OrderStateChangeBean orderStateChangeBean = new OrderStateChangeBean(orderStateChange, order);
        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();

        eventPublisher.publishEvent(new PostStateChangeEvent(this, order, orderStateChangeBean, future));

        final OrderStateChangeBean result = future.get();

        assertThat(result.getPrinterInstructions().isPresent()).isFalse();
    }
}