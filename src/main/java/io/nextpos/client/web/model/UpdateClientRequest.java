package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientRequest {

    @NotBlank
    private String clientName;

    @ValidAttribute
    private Map<String, String> attributes;
}
