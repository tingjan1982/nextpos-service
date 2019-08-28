package io.nextpos.client.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientUserRepository extends JpaRepository<ClientUser, ClientUser.ClientUserId> {

    @Query(value = "select * from client_user where client_id = ?1", nativeQuery = true)
    List<ClientUser> findAllByClientId(String clientId);
}
