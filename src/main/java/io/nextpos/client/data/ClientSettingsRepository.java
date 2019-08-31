package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientSettingsRepository extends JpaRepository<ClientSetting, String> {
}
