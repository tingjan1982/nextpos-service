package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientUserRequest {

    private String nickname;

    @ValidRoles
    private List<String> roles;

    private String userRoleId;

    private List<String> workingAreaIds = new ArrayList<>();
}
