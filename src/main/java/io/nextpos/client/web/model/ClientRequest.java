package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidAttribute;
import io.nextpos.shared.model.validator.ValidPassword;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {

    @NotBlank
    private String clientName;

    @Email
    private String username;

    @ValidPassword
    private String masterPassword;

    @ValidAttribute
    private Map<String, String> attributes;
    
    private ClientInfoRequest clientInfo;
}
