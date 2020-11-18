package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class RefactorOrderTableInfos {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RefactorOrderTableInfos.class);

    private final OrderRepository orderRepository;

    private final ClientRepository clientRepository;

    @Autowired
    public RefactorOrderTableInfos(OrderRepository orderRepository, ClientRepository clientRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
    }

    @Test
    @Rollback(value = false)
    @ChainedTransaction
    void updateOrderTableInfos() {

        final List<String> clientIds = clientRepository.findAll().stream()
                .peek(c -> LOGGER.info("Client[{}] - {}", c.getId(), c.getClientName()))
                .map(Client::getId).collect(Collectors.toList());

        AtomicInteger count = new AtomicInteger();

        StreamSupport.stream(orderRepository.findAll().spliterator(), false)
                .filter(o -> o.getOrderType() == Order.OrderType.IN_STORE)
                .filter(o -> clientIds.contains(o.getClientId()))
                .forEach(o -> {
                    count.incrementAndGet();
                    LOGGER.info("Moving order={}'s table info {} into tables", o.getId(), o.getTableInfo());

                    o.setTables(List.of(o.getTableInfo()));
                    orderRepository.save(o);
                });

        LOGGER.info("Found {} orders with table info", count.get());
    }

    @Test
    @Rollback(value = false)
    @ChainedTransaction
    void removeOrderTableInfo() {


    }
}
