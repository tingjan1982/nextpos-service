package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class DeleteClientServiceImplTest {

    private final DeleteClientService deleteClientService;

    private final ClientService clientService;

    private final OrderService orderService;

    private final OrderRepository orderRepository;

    private final OrderSettings defaultOrderSettings;

    private Client createdClient;

    @Autowired
    DeleteClientServiceImplTest(DeleteClientService deleteClientService, ClientService clientService, OrderService orderService, OrderRepository orderRepository, OrderSettings defaultOrderSettings) {
        this.deleteClientService = deleteClientService;
        this.clientService = clientService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.defaultOrderSettings = defaultOrderSettings;
    }

    @BeforeEach
    public void prepare() {
        final Client client = DummyObjects.dummyClient();
        createdClient = clientService.createClient(client);
    }


    @Test
    void deleteClient() {

        Order order = Order.newOrder(createdClient.getId(), Order.OrderType.IN_STORE, defaultOrderSettings);
        orderService.createOrder(order);

        deleteClientService.deleteClient(createdClient.getId());

        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(clientService.getClient(createdClient.getId())).isEmpty();
    }
}