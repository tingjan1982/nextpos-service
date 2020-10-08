package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SplitAmountDetails {

    private BigDecimal splitAmount;

    private boolean paid;
}
