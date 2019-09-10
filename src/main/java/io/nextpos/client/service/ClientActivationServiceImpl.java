package io.nextpos.client.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.shared.config.ApplicationProperties;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reference:
 * https://www.baeldung.com/registration-verify-user-by-email
 */
@Service
@Transactional
public class ClientActivationServiceImpl implements ClientActivationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientActivationServiceImpl.class);

    private final ClientService clientService;

    private final NotificationService notificationService;

    private final ApplicationProperties applicationProperties;

    private final Configuration freeMarkerCfg;

    @Autowired
    public ClientActivationServiceImpl(final ClientService clientService, final NotificationService notificationService, final ApplicationProperties applicationProperties, final Configuration freeMarkerCfg) {
        this.clientService = clientService;
        this.notificationService = notificationService;
        this.applicationProperties = applicationProperties;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public void initiateClientActivation(Client client) {

        if (applicationProperties.isAutoActivateClient()) {
            LOGGER.info("Client has been activated automatically: {}={}", client.getId(), client.getClientName());

            client.setStatus(Client.Status.ACTIVE);
            clientService.saveClient(client);
        } else {
            LOGGER.info("Sending activation notification to client: {}={}", client.getId(), client.getClientName());

            sendActivationNotification(client);
        }
    }

    @Override
    public CompletableFuture<NotificationDetails> sendActivationNotification(final Client client) {

        try {
            final long timestamp = System.currentTimeMillis();
            String activationToken = String.format("%s=%s", client.getId(), timestamp);
            final String encodedToken = Base64.getEncoder().encodeToString(activationToken.getBytes());
            final String activationLink = String.format("%s/activateaccount?activationToken=%s", resolveHostName(), encodedToken);

            final Template template = freeMarkerCfg.getTemplate("/emailActivation.ftl");
            final StringWriter writer = new StringWriter();
            template.process(Map.of("activationLink", activationLink), writer);

            final EmailDetails emailDetails = new EmailDetails(client.getId(), client.getUsername(), "Nextpos - Account Activation", writer.toString());
            return notificationService.sendNotification(emailDetails);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    String resolveHostName() throws UnknownHostException {

        final String hostname = applicationProperties.getHostname();

        if (StringUtils.isNotEmpty(hostname)) {
            return hostname;
        }
        
        final InetAddress ip = InetAddress.getLocalHost();
        return String.format("http://%s:%d", ip.getHostAddress(), 8080);
    }

    @Override
    public ActivationStatus activateClient(final String encodedToken) {

        final String decodedToken = new String(Base64.getDecoder().decode(encodedToken));
        LOGGER.debug("Decoded token: {}", decodedToken);

        final String[] split = decodedToken.split("=");

        if (split.length != 2) {
            throw new GeneralApplicationException("The decoded token is malformed, possibly due to tempering.");
        }

        final String clientId = split[0];
        final long timestamp = Long.parseLong(split[1]);
        final long now = System.currentTimeMillis();

        if ((now - timestamp) > TimeUnit.DAYS.toMillis(1)) {
            return ActivationStatus.EXPIRED;
        }

        final Optional<Client> clientOptional = clientService.getClient(clientId);

        if (clientOptional.isPresent()) {
            final Client client = clientOptional.get();

            if (client.getStatus() != Client.Status.ACTIVE) {
                clientService.updateClientStatus(client, Client.Status.ACTIVE);
            }

            return ActivationStatus.ACTIVATED;
        }

        return ActivationStatus.FAILED;
    }

    public enum ActivationStatus {
        ACTIVATED, EXPIRED, FAILED
    }
}
