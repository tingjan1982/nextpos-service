package io.nextpos.subscription.scheduler;

import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.service.ClientSubscriptionLifecycleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

@SpringBootTest
@TestPropertySource(properties = {
        "clientSubscription.scheduler.renewActiveClientSubscriptionsCron=*/1 * * * * ?",
        "clientSubscription.scheduler.unpaidSubscriptionInvoicesCron=*/1 * * * * ?",
        "clientSubscription.scheduler.lapseActiveClientSubscriptionsCron=*/1 * * * * ?"
})
class ClientSubscriptionInvoiceSchedulerTest {

    /**
     * https://www.baeldung.com/java-spring-mockito-mock-mockbean
     */
    @MockBean
    private ClientSubscriptionLifecycleService clientSubscriptionLifecycleService;

    @Test
    void testRenewActiveClientSubscriptionsScheduling() throws Exception {

        Thread.sleep(2000);
        Mockito.verify(clientSubscriptionLifecycleService, Mockito.atLeast(1)).renewActiveClientSubscriptions();
    }

    @Test
    void testFindUnpaidSubscriptionInvoicesScheduling() throws Exception {

        Thread.sleep(2000);
        Mockito.verify(clientSubscriptionLifecycleService, Mockito.atLeast(1)).findUnpaidSubscriptionInvoices();
    }

    @Test
    void testProcessActiveLapsingClientSubscriptionsScheduling() throws Exception {

        Thread.sleep(2000);
        Mockito.verify(clientSubscriptionLifecycleService, Mockito.atLeast(1)).processActiveLapsingClientSubscriptions();
    }
}