package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class MergeOrderRequest {

    @NotBlank
    private String orderId;
}
