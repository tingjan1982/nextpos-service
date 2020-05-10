package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@ValidDiscount
public class DiscountRequest {

    private String offerId;

    private String orderDiscount;

    private BigDecimal discount = BigDecimal.ZERO;
}
