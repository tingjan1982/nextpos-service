package io.nextpos.client.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class UpdateClientNameRequest {

    @NotBlank
    private String clientName;
}
