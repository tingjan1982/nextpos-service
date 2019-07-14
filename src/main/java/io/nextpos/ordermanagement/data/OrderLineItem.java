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

        state = OrderLineItemState.OPEN;
        subTotal = new TaxableAmount(taxRate);

        computeSubTotal();
    }

    void updateQuantity(int quantity) {
        this.quantity = quantity;
        computeSubTotal();
    }

    private void computeSubTotal() {

        final BigDecimal optionPriceTotal = productSnapshot.getProductOptions().stream()
                .filter(po -> BigDecimal.ZERO.compareTo(po.getOptionPrice()) < 0)
                .map(ProductSnapshot.ProductOptionSnapshot::getOptionPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal lineItemTotal = productSnapshot.getPrice().add(optionPriceTotal).multiply(BigDecimal.valueOf(quantity));
        subTotal.calculate(lineItemTotal);
    }

    public enum OrderLineItemState {

        OPEN,
        IN_PROCESS,
        DELIVERED
    }
}
