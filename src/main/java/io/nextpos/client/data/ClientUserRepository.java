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

    Optional<ClientUser> findByIdAndClient(ClientUser.ClientUserId id, Client client);

    Optional<ClientUser> findByIdUsernameAndClientIn(String username, List<Client> clients);

    List<ClientUser> findAllByClientIn(List<Client> clients);

    @Modifying
    @Query(value = "delete from client_user where client_id = ?1", nativeQuery = true)
    void deleteClientUsersByClientId(String clientId);
}
