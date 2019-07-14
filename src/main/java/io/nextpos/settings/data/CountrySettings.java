package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Currency;

@Entity(name = "country_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CountrySettings extends BaseObject {

    @Id
    private String isoCountryCode;

    private BigDecimal taxRate;

    private Currency currency;

    public CountrySettings(final String isoCountryCode, final BigDecimal taxRate, final Currency currency) {
        this.isoCountryCode = isoCountryCode;
        this.taxRate = taxRate;
        this.currency = currency;
    }
}
