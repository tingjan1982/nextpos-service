package io.nextpos.linkedaccount.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class LinkedClientAccountRequest {

    @NotBlank
    private String clientUsername;

    @NotBlank
    private String clientPassword;
}
