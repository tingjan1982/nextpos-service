package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClientSettingServiceImplTest {

    @Autowired
    private ClientSettingsService clientSettingsService;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void saveClientSettings() {

        final Client client = DummyObjects.dummyClient();
        clientRepository.save(client);

        final ClientSetting serviceCharge = new ClientSetting(client, ClientSetting.SettingName.SERVICE_CHARGE, "0.1", ClientSetting.ValueType.BIG_DECIMAL, true);
        final ClientSetting savedSettings = clientSettingsService.saveClientSettings(serviceCharge);

        assertThat(savedSettings.getId()).isNotNull();
        assertThat(savedSettings.getClient()).isEqualTo(client);
        assertThat(clientSettingsService.getActualStoredValue(savedSettings, BigDecimal.class)).isInstanceOf(BigDecimal.class);
        assertThatThrownBy(() -> clientSettingsService.getActualStoredValue(savedSettings, String.class)).isInstanceOf(GeneralApplicationException.class);
    }
}