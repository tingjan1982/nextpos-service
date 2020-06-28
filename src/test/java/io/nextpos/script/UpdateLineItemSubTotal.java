package io.nextpos.script;

import io.nextpos.client.data.ClientRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

@Disabled
@SpringBootTest
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration")
public class UpdateLineItemSubTotal {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLineItemSubTotal.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void run() {

        //clientRepository.findAll().stream().filter(c -> c.getUsername().equals("attictradeco@gmail.com")).findFirst().ifPresent(System.out::println);

        mongoTemplate.find(Query.query(Criteria.where("clientId").is("cli-pK57SUracYZILpXFiCaSg8YLgL6F")), Order.class).stream()
                .filter(o -> o.getOrderLineItems().stream()
                        .anyMatch(li -> li.getLineItemSubTotal().equals(BigDecimal.ZERO)))
                .forEach(o -> {
                    LOGGER.info("Updating order {} with total {} created on {}", o.getId(), o.getOrderTotal(), o.getCreatedDate());

                    o.getOrderLineItems().forEach(li -> {
                        System.out.print("Computing total for " + li.getProductSnapshot().getName());
                        final BigDecimal subtotalBefore = li.getLineItemSubTotal();
                        System.out.print(" before: " + subtotalBefore);
                        li.computeSubTotal();
                        final BigDecimal subtotalAfter = li.getLineItemSubTotal();
                        System.out.print(" after: " + subtotalAfter);

                        final boolean different = subtotalAfter.compareTo(subtotalBefore) > 0;
                        System.out.println(" different: " + different);
                    });

                    orderRepository.save(o);
                });

    }
}
