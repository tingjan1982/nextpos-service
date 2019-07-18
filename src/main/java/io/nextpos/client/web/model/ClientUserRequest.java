package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidPassword;
import io.nextpos.shared.model.validator.ValidRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientUserRequest {

    @NotBlank
    private String username;

    @ValidPassword
    private String password;

    @ValidRoles
    private List<String> roles;
}
