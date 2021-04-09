package io.nextpos.script;

import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.reporting.data.DateParameterType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class DeletedOrderSearch {

    private final ClientService clientService;

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    @Autowired
    public DeletedOrderSearch(ClientService clientService, OrderService orderService, OrderTransactionService orderTransactionService) {
        this.clientService = clientService;
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
    }

    @Test
    void run() {
        clientService.getClient("cli-xKIXJ6p49htr0VU3oTNfX5HpAozD").ifPresent(c -> {
            final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(c, DateParameterType.MONTH).build();
            System.out.println(zonedDateRange);

            orderService.getOrders(c, zonedDateRange).stream()
                    .filter(o -> o.getState().equals(Order.OrderState.DELETED))
                    .peek(o -> System.out.println("deleted order: " + o.getId()))
                    .forEach(o -> {
                        orderTransactionService.getOrderTransactionByOrderId(o.getId()).forEach(ot -> {
                            System.out.println("tx order: " + ot.getOrderId());
                            ot.getElectronicInvoice().ifPresent(e -> {
                                System.out.println(e.getInvoiceNumber());
                            });
                        });
                    });
        });
    }
}
