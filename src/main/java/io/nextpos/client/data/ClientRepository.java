package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {

    Optional<Client> findByUsername(String username);

    Optional<Client> findByIdAndStatusIn(String id, Client.Status... status);
}
