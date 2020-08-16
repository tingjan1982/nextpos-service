package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidPassword;
import lombok.Data;

import javax.validation.constraints.Email;

@Data
public class ResetClientPasswordRequest {

    @Email
    private String clientEmail;

    @ValidPassword
    private String password;
}
