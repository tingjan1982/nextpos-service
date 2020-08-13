package io.nextpos.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.nextpos.notification.config.NotificationProperties;
import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.data.NotificationDetailsRepository;
import io.nextpos.notification.data.SmsDetails;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Send email using Mailgun, a trusted 3rd party email service.
 * https://cloud.google.com/compute/docs/tutorials/sending-mail/using-mailgun
 * <p>
 * SMS via Twilio:
 * https://www.twilio.com/docs/sms
 * <p>
 * Baeuldang guide:
 * https://www.baeldung.com/spring-email
 */
@Service
@MongoTransaction
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender javaMailSender;

    private final NotificationDetailsRepository notificationDetailsRepository;

    private final NotificationProperties notificationProperties;

    @Autowired
    public NotificationServiceImpl(final JavaMailSender javaMailSender, final NotificationDetailsRepository notificationDetailsRepository, final NotificationProperties notificationProperties) {
        this.javaMailSender = javaMailSender;
        this.notificationDetailsRepository = notificationDetailsRepository;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public CompletableFuture<NotificationDetails> sendNotification(final NotificationDetails notificationDetails) {

        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Notification is on its way");
            return this.determineSendStrategyAndSend(notificationDetails);
        }).whenComplete((d, t) -> {
            if (t == null) {
                LOGGER.info("Notification has been sent");
                notificationDetails.setDeliveryStatus(NotificationDetails.DeliveryStatus.SUCCESS);
                notificationDetailsRepository.save(notificationDetails);
            }
        }).exceptionally(ex -> {
            LOGGER.error("There were some problems while sending out notification: {}", ex.getMessage(), ex);
            return null;
        });
    }

    private NotificationDetails determineSendStrategyAndSend(NotificationDetails notificationDetails) {
        LOGGER.info("Preparing to send notification: {}", notificationDetails.getId());

        // todo: try out the Rest API implementation
        if (notificationDetails instanceof EmailDetails) {
            try {
                final EmailDetails emailDetails = (EmailDetails) notificationDetails;

                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, Charsets.UTF_8.name());

                helper.setTo(emailDetails.getRecipientEmail());
                helper.setSubject(emailDetails.getSubject());
                helper.setText(emailDetails.getEmailContent(), true);

                javaMailSender.send(message);
            } catch (MessagingException e) {
                LOGGER.error("Problem with sending email: {}", e.getMessage(), e);
                throw new GeneralApplicationException("Problem with sending email: " + e.getMessage());
            }

        } else if (notificationDetails instanceof SmsDetails) {

            final SmsDetails smsDetails = (SmsDetails) notificationDetails;
            final String ACCOUNT_SID = notificationProperties.getAccountSid();
            final String AUTH_TOKEN = notificationProperties.getAuthToken();
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

            Message message = Message.creator(
                    new PhoneNumber(smsDetails.getToNumber()),
                    new PhoneNumber("+15103808519"),
                    smsDetails.getMessage())
                    .create();

            LOGGER.info("SMS Message: {}", message);
        }

        return notificationDetails;
    }
}