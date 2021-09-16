package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.ordermanagement.data.OrderIdCounter;
import io.nextpos.ordermanagement.data.OrderIdCounterRepository;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class OrderCounterServiceImplTest {

    private final OrderCounterService orderCounterService;

    private final ClientService clientService;

    private final OrderIdCounterRepository orderIdCounterRepository;

    @Autowired
    OrderCounterServiceImplTest(OrderCounterService orderCounterService, ClientService clientService, OrderIdCounterRepository orderIdCounterRepository) {
        this.orderCounterService = orderCounterService;
        this.clientService = clientService;
        this.orderIdCounterRepository = orderIdCounterRepository;
    }

    @Test
    void getOrderCounterSummary() {

        final Client client1 = clientService.saveClient(DummyObjects.dummyClient("client1"));
        final Client client2 = clientService.saveClient(DummyObjects.dummyClient("client2"));

        orderIdCounterRepository.save(new OrderIdCounter(client1.getId(), LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), 10));
        orderIdCounterRepository.save(new OrderIdCounter(client1.getId(), LocalDate.now().plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE), 10));
        orderIdCounterRepository.save(new OrderIdCounter(client1.getId(), LocalDate.now().plusDays(2).format(DateTimeFormatter.BASIC_ISO_DATE), 10));
        orderIdCounterRepository.save(new OrderIdCounter(client2.getId(), LocalDate.now().plusDays(3).format(DateTimeFormatter.BASIC_ISO_DATE), 10));
        orderIdCounterRepository.save(new OrderIdCounter(client2.getId(), LocalDate.now().plusDays(4).format(DateTimeFormatter.BASIC_ISO_DATE), 10));
        orderIdCounterRepository.save(new OrderIdCounter(client2.getId(), LocalDate.now().plusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE), 10));

        final Map<String, OrderIdCounter.OrderCounterSummary> orderCounterSummary = orderCounterService.getOrderCounterSummaries();
        assertThat(orderCounterSummary).hasSize(2);
    }
}