package io.nextpos.roles.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRolesResponse {

    private List<UserRoleResponse> userRoles;
}
