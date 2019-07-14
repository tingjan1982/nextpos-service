package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderLineItemRequest {

    @PositiveOrZero
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;
}
