package io.nextpos.client.web.model;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class VerifyClientPasscodeRequest {

    @Email
    private String clientEmail;

    @NotBlank
    private String passcode;
}
