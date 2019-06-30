package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientUserRepository extends JpaRepository<ClientUser, ClientUser.ClientUserId> {

}
