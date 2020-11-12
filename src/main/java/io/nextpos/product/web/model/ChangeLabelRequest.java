package io.nextpos.product.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class ChangeLabelRequest {

    @NotBlank
    private String productLabelId;
}
