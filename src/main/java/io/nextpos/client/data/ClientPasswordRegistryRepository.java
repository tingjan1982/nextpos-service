package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientPasswordRegistryRepository extends JpaRepository<ClientPasswordRegistry, String> {

    Optional<ClientPasswordRegistry> findByClient(Client client);
}
