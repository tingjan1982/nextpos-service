package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {

    Client findByUsername(String username);

    Optional<Client> findByIdAndStatus(String id, Client.Status status);
}
