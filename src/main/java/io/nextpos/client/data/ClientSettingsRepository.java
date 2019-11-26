package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientSettingsRepository extends JpaRepository<ClientSetting, String> {

    Optional<ClientSetting> findByClientAndName(Client client, ClientSetting.SettingName name);

    List<ClientSetting> findAllByClient(Client client);
}
