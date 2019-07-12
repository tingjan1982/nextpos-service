package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CountrySettings extends BaseObject {

    private String isoCountryCode;

    private BigDecimal taxRate;

    public CountrySettings(final String isoCountryCode, final BigDecimal taxRate) {
        this.isoCountryCode = isoCountryCode;
        this.taxRate = taxRate;
    }
}
