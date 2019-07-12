package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.BaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderLineItem extends BaseObject {

    private String id;

    private ProductSnapshot productSnapshot;

    private OrderLineItemState state;

    private int quantity;

    private TaxableAmount subTotal;


    public OrderLineItem(final ProductSnapshot productSnapshot, final int quantity, BigDecimal taxRate) {
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;

        state = OrderLineItemState.NEW;
        subTotal = new TaxableAmount(taxRate);

        computeSubTotal();
    }

    private void computeSubTotal() {
        subTotal.calculate(productSnapshot.getPrice().multiply(BigDecimal.valueOf(quantity)));
    }

    public enum OrderLineItemState {

        NEW,
        OPEN,
        DELIVERED
    }
}
