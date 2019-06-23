package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, String> {

    Client findByClientName(String clientName);
}
