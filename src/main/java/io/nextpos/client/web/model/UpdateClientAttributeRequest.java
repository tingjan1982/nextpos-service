package io.nextpos.client.web.model;

import io.nextpos.shared.model.validator.ValidAttribute;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@NoArgsConstructor
public class UpdateClientAttributeRequest {

    @NotNull
    @ValidAttribute
    private Map<String, String> attributes;
}
