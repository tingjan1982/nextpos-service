package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Currency;

@Data
@AllArgsConstructor
public class OrderSettings {

    private BigDecimal taxRate;

    private boolean taxInclusive;

    private Currency currency;

    private BigDecimal serviceCharge;


    public boolean hasServiceCharge() {
        return serviceCharge != null && serviceCharge.compareTo(BigDecimal.ZERO) > 0;
    }

    public OrderSettings copy() {
        return new OrderSettings(taxRate, taxInclusive, currency, serviceCharge);
    }

}
