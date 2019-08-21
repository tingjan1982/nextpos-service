package io.nextpos.notification.service;

import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationServiceImplTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void sendNotification() throws Exception {

        final EmailDetails emailDetails = new EmailDetails("dummy-id", "tingjan1982@gmail.com",
                "Test from " + NotificationServiceImplTest.class.getName(), "you got mail");

        final CompletableFuture<NotificationDetails> future = notificationService.sendNotification(emailDetails);
        final NotificationDetails savedDetails = future.get(10, TimeUnit.SECONDS);

        assertThat(savedDetails.getId()).isNotNull();
        assertThat(savedDetails.getDeliveryStatus()).isEqualTo(NotificationDetails.DeliveryStatus.SUCCESS);
    }
}