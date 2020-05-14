package io.nextpos.roles.web.model;

import io.nextpos.roles.data.PermissionBundle;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class UserRoleRequest {

    @NotBlank
    private String roleName;

    private Set<PermissionBundle> permissions = new HashSet<>();
}
