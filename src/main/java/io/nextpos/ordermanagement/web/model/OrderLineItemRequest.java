package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.ProductLevelOffer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItemRequest {

    @NotBlank
    private String productId;

    @Positive
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;

    private ProductLevelOffer.GlobalProductDiscount productDiscount;

    private BigDecimal discountValue;
}
