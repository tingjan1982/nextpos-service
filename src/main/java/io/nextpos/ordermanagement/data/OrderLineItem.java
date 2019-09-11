package io.nextpos.ordermanagement.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderLineItem {

    private String id;

    private ProductSnapshot productSnapshot;

    private String workingAreaId;

    private LineItemState state;

    private int quantity;

    private TaxableAmount subTotal;

    private TaxableAmount discountedSubTotal;


    public OrderLineItem(final ProductSnapshot productSnapshot, final int quantity, BigDecimal taxRate) {
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;

        state = LineItemState.OPEN;
        subTotal = new TaxableAmount(taxRate);

        computeSubTotal();
    }

    void updateQuantity(int quantity) {
        this.quantity = quantity;
        
        computeSubTotal();
    }

    /**
     * This method should be called whether line item is changed in the following ways:
     * > new line item is added.
     * > quantity is updated.
     */
    private void computeSubTotal() {

        final BigDecimal productTotal = productSnapshot.getProductPriceWithOptions();
        final BigDecimal lineItemTotal = productTotal.multiply(BigDecimal.valueOf(quantity));

        subTotal.calculate(lineItemTotal);
    }

    public void computeDiscountedSubTotal(BigDecimal discountedProductPrice) {

        productSnapshot.setDiscountedPrice(discountedProductPrice);
        final BigDecimal discountedLineItemTotal = discountedProductPrice.multiply(BigDecimal.valueOf(quantity));

        discountedSubTotal = new TaxableAmount(subTotal.getTaxRate());
        discountedSubTotal.calculate(discountedLineItemTotal);
    }

    public enum LineItemState {

        OPEN,

        IN_PROCESS,

        ALREADY_IN_PROCESS,

        DELIVERED
    }
}
