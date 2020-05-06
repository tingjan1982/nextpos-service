package io.nextpos.roles.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    @EntityGraph(value = "UserRole.clientUsers", type = EntityGraph.EntityGraphType.LOAD)
    @Query("select u from io.nextpos.roles.data.UserRole u where u.id = ?1")
    UserRole loadById(String id);

    List<UserRole> findAllByClientOrderByName(Client client);
}
