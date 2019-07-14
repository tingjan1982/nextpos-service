package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItemRequest {

    private String productId;

    private int quantity;

    private List<OrderProductOptionRequest> productOptions;
}
