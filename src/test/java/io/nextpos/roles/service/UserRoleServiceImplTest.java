package io.nextpos.roles.service;

import io.nextpos.client.data.Client;
import io.nextpos.roles.data.Permission;
import io.nextpos.roles.data.PermissionBundle;
import io.nextpos.roles.data.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRoleServiceImplTest {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private Client client;


    @Test
    void saveUserRole() {

        final UserRole userRole = new UserRole(client, "dummy role");
        userRole.addPermissionBundle(PermissionBundle.CREATE_ORDER);
        userRole.addUserPermission(Permission.CLIENT, Permission.Operation.WRITE);
        userRole.addUserPermission(Permission.ORDER, Permission.Operation.WRITE);

        final UserRole savedUserRole = userRoleService.saveUserRole(userRole);

        assertThat(savedUserRole.getId()).isNotNull();

        assertThat(userRoleService.getUserRole(savedUserRole.getId())).satisfies(ur -> {
            assertThat(ur.getPermissionBundles()).hasSize(1);
            assertThat(ur.getPermissions()).hasSize(2);
            assertThat(ur.getPermissionsAsString()).isEqualTo("write:client,write:order");
        });
    }
}