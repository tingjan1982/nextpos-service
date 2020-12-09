package io.nextpos.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.nextpos.notification.config.NotificationProperties;
import io.nextpos.notification.data.*;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.bson.internal.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Send email using Mailgun, a trusted 3rd party email service.
 * https://cloud.google.com/compute/docs/tutorials/sending-mail/using-mailgun
 * <p>
 * SMS via Twilio:
 * https://www.twilio.com/docs/sms
 * <p>
 * Baeldung guide:
 * https://www.baeldung.com/spring-email
 */
@Service
@MongoTransaction
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender javaMailSender;

    private final NotificationDetailsRepository notificationDetailsRepository;

    private final NotificationProperties notificationProperties;

    private final MailProperties mailProperties;

    @Autowired
    public NotificationServiceImpl(final JavaMailSender javaMailSender, final NotificationDetailsRepository notificationDetailsRepository, final NotificationProperties notificationProperties, final MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.notificationDetailsRepository = notificationDetailsRepository;
        this.notificationProperties = notificationProperties;
        this.mailProperties = mailProperties;
    }

    @Override
    public CompletableFuture<NotificationDetails> sendNotification(final NotificationDetails notificationDetails) {

        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Sending notification to client {}", notificationDetails.getClientId());
            return this.determineSendStrategyAndSend(notificationDetails);

        }).whenComplete((d, t) -> {
            if (t == null) {
                LOGGER.info("Notification has been sent to client {}", notificationDetails.getClientId());
                notificationDetails.setDeliveryStatus(NotificationDetails.DeliveryStatus.SUCCESS);
                notificationDetailsRepository.save(notificationDetails);
            }
        }).exceptionally(ex -> {
            LOGGER.error("Error while sending notification to client {}: {}", notificationDetails.getClientId(), ex.getMessage(), ex);
            notificationDetails.setDeliveryStatus(NotificationDetails.DeliveryStatus.FAIL);
            notificationDetailsRepository.save(notificationDetails);

            return null;
        });
    }

    private NotificationDetails determineSendStrategyAndSend(NotificationDetails notificationDetails) {
        LOGGER.info("Preparing to send {} notification", notificationDetails.getClass().getSimpleName());

        if (notificationDetails instanceof DynamicEmailDetails) {
            sendDynamicEmailNotification(((DynamicEmailDetails) notificationDetails));

        } else if (notificationDetails instanceof EmailDetails) {
            sendEmailNotification((EmailDetails) notificationDetails);

        } else if (notificationDetails instanceof SmsDetails) {
            sendSmsNotification((SmsDetails) notificationDetails);
        }

        return notificationDetails;
    }

    private void sendDynamicEmailNotification(DynamicEmailDetails notificationDetails) {

        try {
            Email from = new Email("notification-noreply@rain-app.io");
            Email to = new Email(notificationDetails.getRecipientEmail());
            Mail mail = new Mail();
            mail.setTemplateId(notificationDetails.getTemplateId());
            mail.setFrom(from);

            final Personalization personalization = new Personalization();
            personalization.addTo(to);

            notificationDetails.getTemplateData().forEach(personalization::addDynamicTemplateData);
            mail.addPersonalization(personalization);

            if (notificationDetails.getAttachment() != null) {
                final Attachments attachments = new Attachments();
                attachments.setContent(Base64.encode(notificationDetails.getAttachment().getData()));
                attachments.setType("application/pdf");
                attachments.setFilename("einvoice.pdf");
                attachments.setDisposition("attachment");
                attachments.setContentId("Banner");

                mail.addAttachments(attachments);
            }

            SendGrid sg = new SendGrid(mailProperties.getPassword());
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            LOGGER.info("{}", response);

        } catch (Exception e) {
            String errorMsg = String.format("Error while sending email: %s", e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new GeneralApplicationException(errorMsg);
        }
    }

    private void sendEmailNotification(EmailDetails notificationDetails) {

        try {
            Email from = new Email("notification-noreply@rain-app.io");
            Email to = new Email(notificationDetails.getRecipientEmail());
            Content content = new Content("text/html", notificationDetails.getEmailContent());
            Mail mail = new Mail(from, notificationDetails.getSubject(), to, content);

            SendGrid sg = new SendGrid(mailProperties.getPassword());
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            LOGGER.info("{}", response);

        } catch (Exception e) {
            String errorMsg = String.format("Error while sending email: %s", e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new GeneralApplicationException(errorMsg);
        }
    }

    private void sendSmsNotification(SmsDetails notificationDetails) {

        final String ACCOUNT_SID = notificationProperties.getAccountSid();
        final String AUTH_TOKEN = notificationProperties.getAuthToken();
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message.creator(
                new PhoneNumber(notificationDetails.getToNumber()),
                new PhoneNumber("+15103808519"),
                notificationDetails.getMessage())
                .create();

        LOGGER.info("SMS Message: {}", message);
    }
}