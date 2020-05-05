package io.nextpos.roles.web.model;

import io.nextpos.roles.data.PermissionBundle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRoleResponse {

    private String id;

    private String roleName;

    private List<PermissionBundle> permissions;

}
