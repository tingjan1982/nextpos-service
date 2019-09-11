package io.nextpos.ordermanagement.event;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostOrderStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CountrySettings defaultCountrySettings;

    private Client client;

    private Order order;


    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientRepository.save(client);

        final WorkingArea workingArea = new WorkingArea(client, "bar");
        workingAreaService.saveWorkingArea(workingArea);

        order = new Order(client.getId(), defaultCountrySettings.getTaxRate(), defaultCountrySettings.getCurrency());
        order.setState(Order.OrderState.IN_PROCESS);

        final OrderLineItem item1 = new OrderLineItem(DummyObjects.productSnapshot(), 2, defaultCountrySettings.getTaxRate());
        item1.setWorkingAreaId(workingArea.getId());
        item1.setState(OrderLineItem.LineItemState.IN_PROCESS);

        order.addOrderLineItem(item1);
        orderService.saveOrder(order);
    }

    // todo: add one more test to test order not in process.
    @Test
    void postOrderStateChange() throws Exception {

        final OrderStateChange orderStateChange = new OrderStateChange(order.getId(), client.getId());
        final OrderStateChangeBean orderStateChangeBean = new OrderStateChangeBean(orderStateChange);
        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();

        eventPublisher.publishEvent(new PostStateChangeEvent(this, order, orderStateChangeBean, future));

        final OrderStateChangeBean result = future.get();

        assertThat(result.getPrinterInstructions()).isNotNull();
    }
}