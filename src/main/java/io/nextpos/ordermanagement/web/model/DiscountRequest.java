package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
//@ValidDiscount
public class DiscountRequest implements OrderLogChangeObject {

    @NotBlank
    private String offerId;

    private String orderDiscount;

    private BigDecimal discount = BigDecimal.ZERO;

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        orderLog.addChangeOrderLogEntry(() -> OrderLogProvider.appliedOfferInfoLog(orderBeforeChange.getAppliedOfferInfo(), orderAfterChange.getAppliedOfferInfo()));
    }
}
