package io.nextpos.roles.web.model;

import io.nextpos.roles.data.PermissionBundle;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class UserRoleRequest {

    @NotBlank
    private String roleName;

    private List<PermissionBundle> permissions = new ArrayList<>();
}
