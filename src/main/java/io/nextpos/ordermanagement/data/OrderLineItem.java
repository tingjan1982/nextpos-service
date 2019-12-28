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


    public OrderLineItem(final ProductSnapshot productSnapshot, final int quantity, OrderSettings orderSettings) {
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;

        state = LineItemState.OPEN;
        this.subTotal = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());

        computeSubTotal();
    }

    public TaxableAmount getProductPriceWithOptions() {

        final BigDecimal productTotal = productSnapshot.getProductPriceWithOptions();
        final TaxableAmount taxableProductPrice = subTotal.newInstance();
        taxableProductPrice.calculate(productTotal);

        return taxableProductPrice;
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

        final BigDecimal computedDiscount = this.replayOfferIfExists(this.getProductPriceWithOptions());
        applyOffer(computedDiscount);
    }

    @Override
    public void applyOffer(final BigDecimal computedDiscount) {

        productSnapshot.setDiscountedPrice(computedDiscount);
        final BigDecimal discountedLineItemTotal = computedDiscount.multiply(BigDecimal.valueOf(quantity));

        discountedSubTotal = subTotal.newInstance();
        discountedSubTotal.calculate(discountedLineItemTotal);
    }

    /**
     * For tax inclusive scenarios, return amountWithTax because tax will not be calculated again, whereas in tax exclusive scenarios,
     * return amountWithoutTax so an Order will aggregate and calculate tax as a whole.
     * @return
     */
    public BigDecimal getLineItemSubTotal() {

        final TaxableAmount subTotal = discountedSubTotal != null && !discountedSubTotal.isZero() ? discountedSubTotal : this.subTotal;
        return subTotal.getAmount();
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
