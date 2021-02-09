package io.nextpos.client.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class UpdateClientUsernameRequest {

    @Email
    private String newUsername;

    @NotBlank
    private String password;
}
