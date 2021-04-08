package io.nextpos.client.service;

import com.google.common.base.Splitter;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.bean.ClientAuthToken;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Encryption reference:
 *
 * https://docs.spring.io/spring-security/site/docs/5.0.x/reference/html/crypto.html
 */
@Service
@ChainedTransaction
public class ClientAuthTokenServiceImpl implements ClientAuthTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientAuthTokenServiceImpl.class);

    private static final String ENCODER_SALT = "95b98c97953a3a4e";

    private static final String ENCODER_PASSWORD = "rainapp";

    static final String SEPARATOR = "[S]";

    static final long VALIDITY_PERIOD = TimeUnit.MINUTES.toMillis(10);

    private final ClientService clientService;

    private final TextEncryptor commonEncryptor;

    public ClientAuthTokenServiceImpl(ClientService clientService) {
        this.clientService = clientService;
        this.commonEncryptor = this.createTextEncryptor();
    }

    TextEncryptor createTextEncryptor() {
        return Encryptors.text(ENCODER_PASSWORD, ENCODER_SALT);
    }

    @Override
    public String encodeClientAuthToken(Client client, String password) {

        final TextEncryptor encryptor = getEncryptor(client);

        String token = System.currentTimeMillis() + SEPARATOR + client.getUsername() + SEPARATOR + encryptor.encrypt(password);
        return commonEncryptor.encrypt(token);
    }

    @Override
    public ClientAuthToken decodeClientAuthToken(String encodedToken) {

        try {
            final String decodedToken = commonEncryptor.decrypt(encodedToken);
            final List<String> splits = Splitter.on(SEPARATOR).splitToList(decodedToken);

            if (splits.size() != 3) {
                throw new BusinessLogicException("Invalid token");
            }

            long timestamp = Long.parseLong(splits.get(0));
            final long currentTimestamp = System.currentTimeMillis();

            if (currentTimestamp - timestamp > VALIDITY_PERIOD) {
                throw new BusinessLogicException("Token has expired");
            }

            final String username = splits.get(1);
            final Client client = clientService.getClientByUsernameOrThrows(username);
            final TextEncryptor encryptor = getEncryptor(client);
            final String decryptedPassword = encryptor.decrypt(splits.get(2));

            return new ClientAuthToken(username, decryptedPassword);

        } catch (Exception e) {
            LOGGER.error("{}", e.getMessage(), e);
            throw new BusinessLogicException("message.decodeTokenFailed", "Error decoding token: " + e.getMessage());
        }
    }

    private TextEncryptor getEncryptor(Client client) {

        String salt = client.getSalt();

        if (StringUtils.isBlank(salt)) {
            salt = KeyGenerators.string().generateKey();
            client.setSalt(salt);
            clientService.saveClient(client);
        }

        return Encryptors.text(client.getUsername(), salt);
    }
}
