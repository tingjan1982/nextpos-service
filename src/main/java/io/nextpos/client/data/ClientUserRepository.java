package io.nextpos.client.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientUserRepository extends JpaRepository<ClientUser, String> {

    @EntityGraph(value = "ClientUser.userRole", type = EntityGraph.EntityGraphType.LOAD)
    @Query("select u from io.nextpos.client.data.ClientUser u where u.client = ?1 and u.username = ?2")
    Optional<ClientUser> loadById(Client client, String username);

    @EntityGraph(value = "ClientUser.userRole", type = EntityGraph.EntityGraphType.LOAD)
    @Query("select u from io.nextpos.client.data.ClientUser u where u.id = ?1")
    Optional<ClientUser> loadById(String id);

    Optional<ClientUser> findByClientAndUsername(Client client, String username);

    Optional<ClientUser> findByUsernameAndClientIn(String username, List<Client> clients);

    List<ClientUser> findAllByClientIn(List<Client> clients);
    
    void deleteAllByClient(Client client);
}
