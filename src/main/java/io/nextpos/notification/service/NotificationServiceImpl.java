package io.nextpos.notification.service;

import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.data.NotificationDetailsRepository;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
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
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender javaMailSender;

    private final NotificationDetailsRepository notificationDetailsRepository;

    @Autowired
    public NotificationServiceImpl(final JavaMailSender javaMailSender, final NotificationDetailsRepository notificationDetailsRepository) {
        this.javaMailSender = javaMailSender;
        this.notificationDetailsRepository = notificationDetailsRepository;
    }

    @Override
    public CompletableFuture<NotificationDetails> sendNotification(final NotificationDetails notificationDetails) {

        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Notification is on its way");
            return this.determineSendStrategyAndSend(notificationDetails);
        }).whenComplete((d, t) -> {
            LOGGER.info("Notification has been sent");
            notificationDetails.setDeliveryStatus(NotificationDetails.DeliveryStatus.SUCCESS);
            notificationDetailsRepository.save(notificationDetails);
        }).exceptionally(ex -> {
            LOGGER.error("There were some problems while sending out notification: {}", ex.getMessage(), ex);
            return null;
        });
    }

    private NotificationDetails determineSendStrategyAndSend(NotificationDetails notificationDetails) {
        LOGGER.info("Preparing to send notification: {}", notificationDetails);

        if (notificationDetails instanceof EmailDetails) {
            try {
                final EmailDetails emailDetails = (EmailDetails) notificationDetails;

                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, Charsets.UTF_8.name());

                //final SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
                helper.setTo(emailDetails.getRecipientEmail());
                helper.setSubject(emailDetails.getSubject());
                helper.setText(emailDetails.getEmailContent(), true);

                javaMailSender.send(message);
            } catch (MessagingException e) {
                LOGGER.error("Problem with sending email: {}", e.getMessage(), e);
                throw new GeneralApplicationException("Problem with sending email: " + e.getMessage());
            }

//            Client client = Client.create();
//            client.addFilter(new HTTPBasicAuthFilter("api", "c74822277520a5a9533ea1b40693e414-2ae2c6f3-24964f0f"));
//            WebResource webResource = client.resource("https://api.mailgun.net/v3/sandboxdb5a2b7445a8403a8085bc2b50852b7a.mailgun.org/messages");
//            MultivaluedMapImpl formData = new MultivaluedMapImpl();
//            formData.add("from", "Mailgun Sandbox <postmaster@sandboxdb5a2b7445a8403a8085bc2b50852b7a.mailgun.org>");
//            formData.add("to", "Joe Lin <tingjan1982@gmail.com>");
//            formData.add("subject", "Hello Joe Lin");
//            formData.add("text", "Congratulations Joe Lin, you just sent an email with Mailgun!  You are truly awesome!");
//            return webResource.type(MediaType.APPLICATION_FORM_URLENCODED).
//                    post(ClientResponse.class, formData);

//            Properties props = System.getProperties();
//            props.put("mail.smtps.host", "smtp.mailgun.org");
//            props.put("mail.smtps.auth", "true");
        }

        return notificationDetails;
    }
}
