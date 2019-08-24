package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientUserRequest {

    @NotBlank
    private String username;

    @Size(min = 4, max = 8)
    @Pattern(regexp="^([0-9]*)$")
    private String password;

    @ValidRoles
    private List<String> roles;
}
