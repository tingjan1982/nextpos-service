package io.nextpos.client.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientUserRepository extends JpaRepository<ClientUser, ClientUser.ClientUserId> {

    @EntityGraph(value = "ClientUser.userRole", type = EntityGraph.EntityGraphType.LOAD)
    @Query("select u from io.nextpos.client.data.ClientUser u where u.id = ?1")
    Optional<ClientUser> loadById(ClientUser.ClientUserId id);

    @Query(value = "select * from client_user where client_id = ?1 order by nickname asc, username asc", nativeQuery = true)
    List<ClientUser> findAllByClientId(String clientId);

    @Modifying
    @Query(value = "delete from client_user where client_id = ?1", nativeQuery = true)
    void deleteClientUsersByClientId(String clientId);
}
