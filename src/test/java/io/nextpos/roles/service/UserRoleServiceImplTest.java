package io.nextpos.roles.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roles.data.Permission;
import io.nextpos.roles.data.PermissionBundle;
import io.nextpos.roles.data.UserRole;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRoleServiceImplTest {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private Client client;


    @Test
    void saveUserRole() {

        final UserRole userRole = new UserRole(client, "dummy role");
        userRole.addPermissionBundle(PermissionBundle.CREATE_ORDER);
        userRole.addUserPermission(Permission.CLIENT, Permission.Operation.WRITE);

        final UserRole savedUserRole = userRoleService.saveUserRole(userRole);

        assertThat(savedUserRole.getId()).isNotNull();

        assertThat(userRoleService.getUserRole(savedUserRole.getId())).satisfies(ur -> {
            assertThat(ur.getPermissionBundles()).hasSize(2);
            assertThat(ur.getPermissions()).isNotEmpty();
        });

        final ClientUser clientUser = DummyObjects.dummyClientUser(client);
        clientUser.setUserRole(savedUserRole);

        final ClientUser savedClientUser = clientService.saveClientUser(clientUser);

        assertThat(savedClientUser.getUserRole()).isEqualTo(userRole);
        assertThat(savedClientUser.getPermissions()).isEqualTo(userRole.getPermissionsAsString());

        userRole.setName("updated role");
        userRole.updatePermissionBundle(Set.of(PermissionBundle.CREATE_ORDER, PermissionBundle.DELETE_ORDER));

        final UserRole updatedUserRole = userRoleService.updateUserRole(userRole);

        assertThat(updatedUserRole.getPermissionBundles()).hasSize(3);
        assertThat(updatedUserRole.getPermissions()).isNotNull();

        final ClientUser userToCheck = clientService.getClientUser(client, clientUser.getUsername());

        assertThat(userToCheck.getUserRole().getPermissionBundles()).hasSize(updatedUserRole.getPermissionBundles().size());
        assertThat(userToCheck.getUserRole().getPermissions()).hasSize(updatedUserRole.getPermissions().size());
        assertThat(userToCheck.getUserRole()).isEqualTo(updatedUserRole);

        userRoleService.removeClientUserRole(userToCheck);

        final ClientUser removedRoleUser = clientService.getClientUser(client, userToCheck.getUsername());
        final UserRole updatedRole2 = userRoleService.getUserRole(updatedUserRole.getId());

        assertThat(removedRoleUser.getUserRole()).isNull();
        assertThat(removedRoleUser.getPermissions()).isBlank();
        assertThat(updatedRole2.getClientUsers()).isEmpty();
    }
}