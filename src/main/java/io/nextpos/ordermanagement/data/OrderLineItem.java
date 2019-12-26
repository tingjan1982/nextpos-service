package io.nextpos.ordermanagement.data;

import io.nextpos.merchandising.data.OfferApplicableObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderLineItem implements OfferApplicableObject {

    private String id;

    private ProductSnapshot productSnapshot;

    private String workingAreaId;

    private LineItemState state;

    private int quantity;

    /**
     * SubTotal of product price x quantity.
     */
    private TaxableAmount subTotal;

    /**
     * Subtotal of discounted product price x quantity.
     */
    private TaxableAmount discountedSubTotal;

    private AppliedOfferInfo appliedOfferInfo;


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
        computeDiscountedSubTotal();
    }

    @Override
    public void applyOffer(final BigDecimal computedDiscount) {

        setDiscountedProductPrice(computedDiscount);
        computeDiscountedSubTotal();
    }

    public void setDiscountedProductPrice(BigDecimal discountedProductPrice) {
        productSnapshot.setDiscountedPrice(discountedProductPrice);
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

    public void computeDiscountedSubTotal() {

        final BigDecimal discountedProductPrice = productSnapshot.getDiscountedPrice();

        if (discountedProductPrice != null) {
            final BigDecimal discountedLineItemTotal = discountedProductPrice.multiply(BigDecimal.valueOf(quantity));

            discountedSubTotal = new TaxableAmount(subTotal.getTaxRate());
            discountedSubTotal.calculate(discountedLineItemTotal);
        }
    }

    public BigDecimal getLineItemSubTotal() {

        final TaxableAmount subTotal = discountedSubTotal != null ? discountedSubTotal : this.subTotal;
        return subTotal.getAmountWithoutTax();
    }

    public OrderLineItem copy() {
        final OrderLineItem copy = new OrderLineItem();
        copy.productSnapshot = productSnapshot.copy();
        copy.workingAreaId = workingAreaId;
        copy.state = state;
        copy.quantity = quantity;
        copy.subTotal = subTotal.copy();
        copy.discountedSubTotal = discountedSubTotal != null ? discountedSubTotal.copy() : null;

        return copy;
    }

    public enum LineItemState {

        OPEN,

        IN_PROCESS,

        ALREADY_IN_PROCESS,

        DELIVERED
    }
}
