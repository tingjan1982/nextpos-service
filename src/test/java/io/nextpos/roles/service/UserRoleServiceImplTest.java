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

import javax.transaction.Transactional;
import java.util.List;

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
            assertThat(ur.getPermissions()).hasSize(14);
            assertThat(ur.getPermissionsAsString()).isEqualTo("write:order,read:client,read:client_user,read:time_card,write:time_card,read:product,read:order,read:shift,read:table_layout,read:table,read:working_area,read:printer,read:announcement,write:client");
        });

        final ClientUser clientUser = DummyObjects.dummyClientUser();
        clientUser.setUserRole(savedUserRole);

        final ClientUser savedClientUser = clientService.saveClientUser(clientUser);

        assertThat(savedClientUser.getUserRole()).isEqualTo(userRole);
        assertThat(savedClientUser.getPermissions()).isEqualTo(userRole.getPermissionsAsString());

        final UserRole updatedUserRole = userRoleService.updateUserRole(userRole, "updated role", List.of(PermissionBundle.CREATE_ORDER, PermissionBundle.DELETE_ORDER));

        assertThat(updatedUserRole.getPermissionBundles()).hasSize(3);
        assertThat(updatedUserRole.getPermissions()).hasSize(14);

        final ClientUser userToCheck = clientService.getClientUser(client, clientUser.getId().getUsername());

        assertThat(userToCheck.getUserRole().getPermissionBundles()).hasSize(updatedUserRole.getPermissionBundles().size());
        assertThat(userToCheck.getUserRole().getPermissions()).hasSize(updatedUserRole.getPermissions().size());
        assertThat(userToCheck.getUserRole()).isEqualTo(updatedUserRole);
    }
}