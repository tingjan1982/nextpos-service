package io.nextpos.linkedaccount.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkedClientAccountRepository extends JpaRepository<LinkedClientAccount, String> {

    Optional<LinkedClientAccount> findBySourceClient(Client sourceClient);

    Optional<LinkedClientAccount> findByLinkedClients(Client linkedClient);
}
