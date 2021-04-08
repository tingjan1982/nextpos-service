package io.nextpos.client.service;

import com.google.common.base.Splitter;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.bean.ClientAuthToken;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ChainedTransaction
class ClientAuthTokenServiceImplTest {

    private final ClientAuthTokenServiceImpl clientAuthTokenService;

    private final Client client;

    @Autowired
    ClientAuthTokenServiceImplTest(ClientAuthTokenServiceImpl clientAuthTokenService, Client client) {
        this.clientAuthTokenService = clientAuthTokenService;
        this.client = client;
    }

    @Test
    void encodeAndDecode() {

        final String password = "Secret1";
        final String token = clientAuthTokenService.encodeClientAuthToken(client, password);

        assertThat(client.getSalt()).isNotNull();

        final ClientAuthToken clientAuthToken = clientAuthTokenService.decodeClientAuthToken(token);

        assertThat(clientAuthToken.getUsername()).isEqualTo(client.getUsername());
        assertThat(clientAuthToken.getPassword()).isEqualTo(password);
    }

    @Test
    void handleInvalidTokens() {

        final String token = clientAuthTokenService.encodeClientAuthToken(client, "dummy");
        final TextEncryptor textEncryptor = clientAuthTokenService.createTextEncryptor();
        final String decodedToken = textEncryptor.decrypt(token);
        final List<String> splits = Splitter.on(ClientAuthTokenServiceImpl.SEPARATOR).splitToList(decodedToken);
        final long temperedTimestamp = System.currentTimeMillis() - ClientAuthTokenServiceImpl.VALIDITY_PERIOD - TimeUnit.SECONDS.toMillis(5);
        final String updatedToken = "" + temperedTimestamp + ClientAuthTokenServiceImpl.SEPARATOR + splits.get(1) + ClientAuthTokenServiceImpl.SEPARATOR + splits.get(2);
        final String encodedToken = textEncryptor.encrypt(updatedToken);

        assertThatThrownBy(() -> clientAuthTokenService.decodeClientAuthToken(encodedToken)).isInstanceOf(BusinessLogicException.class)
                .hasMessage("Token has expired");
    }
}