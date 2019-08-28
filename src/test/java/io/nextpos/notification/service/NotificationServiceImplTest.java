package io.nextpos.notification.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.data.SmsDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reference on testing sending email capability with Greenmail:
 * 
 * https://memorynotfound.com/spring-mail-integration-testing-junit-greenmail-example/
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceImplTest {

    @Autowired
    private NotificationService notificationService;

    private static GreenMail smtpServer;

    @BeforeAll
    static void beforeAll() {
        smtpServer = new GreenMail(new ServerSetup(2525, null, "smtp"));
        smtpServer.setUser("username", "password");

        smtpServer.start();
    }

    @AfterAll
    static void afterAll() {
        smtpServer.stop();
    }

    @Test
    void sendNotification() throws Exception {

        final EmailDetails emailDetails = new EmailDetails("dummy-id", "tingjan1982@gmail.com",
                "Test from " + NotificationServiceImplTest.class.getName(), "you got mail");

        final CompletableFuture<NotificationDetails> future = notificationService.sendNotification(emailDetails);
        final NotificationDetails savedDetails = future.get(10, TimeUnit.SECONDS);

        assertThat(savedDetails.getId()).isNotNull();
        assertThat(savedDetails.getDeliveryStatus()).isEqualTo(NotificationDetails.DeliveryStatus.SUCCESS);
    }

    /**
     * remove @Disabled to test SMS capability.
     */
    @Test
    @Disabled
    void sendSmsNotification() throws Exception {

        final SmsDetails smsDetails = new SmsDetails("dummy-id", "from", "+886988120232", "test message");

        final CompletableFuture<NotificationDetails> future = notificationService.sendNotification(smsDetails);

        final NotificationDetails savedDetails = future.get(10, TimeUnit.SECONDS);

        assertThat(savedDetails.getId()).isNotNull();
        assertThat(savedDetails.getDeliveryStatus()).isEqualTo(NotificationDetails.DeliveryStatus.SUCCESS);
    }
}