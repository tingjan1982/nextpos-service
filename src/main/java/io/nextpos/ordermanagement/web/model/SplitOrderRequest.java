package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class SplitOrderRequest {

    @NotBlank
    private String sourceOrderId;

    @NotBlank
    private String sourceLineItemId;
}
