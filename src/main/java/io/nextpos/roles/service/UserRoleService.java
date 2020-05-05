package io.nextpos.roles.service;

import io.nextpos.client.data.Client;
import io.nextpos.roles.data.UserRole;

import java.util.List;

public interface UserRoleService {

    UserRole saveUserRole(UserRole userRole);

    UserRole getUserRole(String id);

    List<UserRole> getUserRoles(Client client);

    UserRole updateUserRole(UserRole userRole);

    void deleteUserRole(UserRole userRole);

}
