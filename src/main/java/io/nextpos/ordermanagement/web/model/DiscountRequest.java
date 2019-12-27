package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.shared.model.validator.ValidEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class DiscountRequest {

    @ValidEnum(enumType = OrderLevelOffer.GlobalOrderDiscount.class)
    private String orderDiscount;

    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax("100")
    private BigDecimal discount;
}
