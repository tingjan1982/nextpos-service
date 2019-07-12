package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private List<OrderLineItemRequest> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemRequest {

        private String productId;

        private List<OrderProductOptionRequest> productOptions;

        private int quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderProductOptionRequest {

        private String optionName;

        private String optionValue;

        private BigDecimal optionPrice;
    }
}
