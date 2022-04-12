package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientActivationResult;
import io.nextpos.client.data.ClientUser;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.data.EmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.shared.config.ApplicationProperties;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reference:
 * https://www.baeldung.com/registration-verify-user-by-email
 */
@Service
@ChainedTransaction
public class ClientActivationServiceImpl implements ClientActivationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientActivationServiceImpl.class);

    private final ClientService clientService;

    private final NotificationService notificationService;

    private final ApplicationProperties applicationProperties;

    @Autowired
    public ClientActivationServiceImpl(final ClientService clientService, final NotificationService notificationService, final ApplicationProperties applicationProperties) {
        this.clientService = clientService;
        this.notificationService = notificationService;
        this.applicationProperties = applicationProperties;
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
            final String activationLink = String.format("%s/account/activateaccount?activationToken=%s", resolveHostName(), encodedToken);

            final DynamicEmailDetails dynamicEmailDetails = new DynamicEmailDetails(client.getId(), client.getUsername(), "d-084e3b83897e471d85e627f3d56e7c80");
            dynamicEmailDetails.addTemplateData("client", client.getClientName());
            dynamicEmailDetails.addTemplateData("activationLink", activationLink);

            return notificationService.sendNotification(dynamicEmailDetails);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    String resolveHostName() {

        try {
            final String hostname = applicationProperties.getHostname();

            if (StringUtils.isNotEmpty(hostname)) {
                return hostname;
            }

            final InetAddress ip = InetAddress.getLocalHost();
            return String.format("http://%s:%d", ip.getHostAddress(), 8080);

        } catch (Exception e) {
            throw new GeneralApplicationException("Unable to resolve host name: " + e.getMessage());
        }
    }

    @Override
    public ClientActivationResult activateClient(final String encodedToken) {

        final String decodedToken = new String(Base64.getDecoder().decode(encodedToken));
        LOGGER.debug("Decoded token: {}", decodedToken);

        final String[] split = decodedToken.split("=");

        if (split.length != 2) {
            throw new GeneralApplicationException("The decoded token is malformed, possibly due to tempering.");
        }

        final String clientId = split[0];
        final Optional<Client> clientOptional = clientService.getClient(clientId);

        if (clientOptional.isEmpty()) {
            return new ClientActivationResult(clientId, ClientActivationResult.ActivationStatus.FAILED);
        }

        final long timestamp = Long.parseLong(split[1]);
        final long now = System.currentTimeMillis();

        if ((now - timestamp) > TimeUnit.DAYS.toMillis(1)) {
            final String resendClientActivationLink = resolveHostName() + "/account/resendClientActivation?clientId=" + clientId;
            final ClientActivationResult result = new ClientActivationResult(clientId, ClientActivationResult.ActivationStatus.EXPIRED);
            result.setClientActivationLink(resendClientActivationLink);

            return result;
        }

        final Client client = clientOptional.get();

        if (client.getStatus() != Client.Status.ACTIVE) {
            clientService.updateClientStatus(client, Client.Status.ACTIVE);
        }

        return new ClientActivationResult(clientId, ClientActivationResult.ActivationStatus.ACTIVATED);
    }

    @Override
    public CompletableFuture<NotificationDetails> sendResetPasscode(String clientEmail) {

        final Client client = clientService.getClientByUsername(clientEmail).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientEmail, Client.class);
        });

        final String passcode = generatePasscode();
        client.addAttribute(Client.ClientAttributes.PASSCODE, passcode);
        clientService.saveClient(client);

        final EmailDetails emailDetails = new EmailDetails(client.getId(), client.getNotificationEmail(null), "Rain App Reset Password", passcode);

        return notificationService.sendNotification(emailDetails);
    }

    @Override
    public boolean verifyResetPasscode(String clientEmail, String passcodeToVerify) {

        final Client client = clientService.getClientByUsername(clientEmail).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientEmail, Client.class);
        });

        final String passcode = client.getAttribute(Client.ClientAttributes.PASSCODE.name());
        final boolean verified = StringUtils.equals(passcodeToVerify, passcode);

        if (verified) {
            client.removeAttribute(Client.ClientAttributes.PASSCODE.name());
            client.addAttribute(Client.ClientAttributes.PASSCODE_VERIFIED, String.valueOf(System.currentTimeMillis()));
        }

        return verified;
    }

    @Override
    public ClientUser resetClientPassword(final String clientEmail, final String password) {

        final Client client = clientService.getClientByUsername(clientEmail).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientEmail, Client.class);
        });

        final String verifiedTimestamp = client.getAttribute(Client.ClientAttributes.PASSCODE_VERIFIED.name());

        if (StringUtils.isBlank(verifiedTimestamp)) {
            throw new BusinessLogicException("Reset client password operation is not authorized");
        }

        final long timestamp = Long.parseLong(verifiedTimestamp);
        final long lapsedTime = System.currentTimeMillis() - timestamp;
        final Duration duration = Duration.of(lapsedTime, ChronoUnit.MILLIS);

        if (duration.getSeconds() > 300) {
            throw new BusinessLogicException("Reset client password operation is expired");
        }

        final ClientUser defaultClientUser = clientService.getClientUser(client, clientEmail);

        return clientService.updateClientUserPassword(client, defaultClientUser, password);
    }

    private String generatePasscode() {
        return RandomStringUtils.randomNumeric(6);
    }
}
