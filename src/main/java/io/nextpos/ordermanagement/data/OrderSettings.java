package io.nextpos.ordermanagement.data;

import io.nextpos.settings.data.CountrySettings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSettings {

    private BigDecimal taxRate;

    private boolean taxInclusive;

    private Currency currency;

    private BigDecimal serviceCharge;

    private int decimalPlaces;

    private RoundingMode roundingMode;

    public OrderSettings(CountrySettings countrySettings, boolean taxInclusive, BigDecimal serviceCharge) {
        this.taxRate = countrySettings.getTaxRate();
        this.currency = countrySettings.getCurrency();
        this.decimalPlaces = countrySettings.getDecimalPlaces();
        this.roundingMode = countrySettings.getRoundingMode();

        this.taxInclusive = taxInclusive;
        this.serviceCharge = serviceCharge;
    }

    /**
     * This handling is to accommodate the cases where orders have been previously created without rounding mode.
     *
     * @return
     */
    public RoundingMode getRoundingMode() {
        return roundingMode != null ? roundingMode : RoundingMode.HALF_UP;
    }

    public boolean hasServiceCharge() {
        return serviceCharge != null && serviceCharge.compareTo(BigDecimal.ZERO) > 0;
    }

    public OrderSettings copy() {
        return new OrderSettings(taxRate, taxInclusive, currency, serviceCharge, decimalPlaces, roundingMode);
    }

}
