package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductOptionRequest {

    private String optionName;

    private String optionValueId;

    private String optionValue;

    private BigDecimal optionPrice;
}
