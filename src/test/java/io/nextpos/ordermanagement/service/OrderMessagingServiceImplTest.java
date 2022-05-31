package io.nextpos.ordermanagement.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled
@SpringBootTest
class OrderMessagingServiceImplTest {

    private final OrderMessagingService orderMessagingService;

    @Autowired
    OrderMessagingServiceImplTest(OrderMessagingService orderMessagingService) {
        this.orderMessagingService = orderMessagingService;
    }

    /**
     * This test requires the message service to be up and running.
     */
    @Test
    void sendInFlightOrderUpdate() {

        orderMessagingService.sendInFlightOrderUpdate("test-client");
    }
}