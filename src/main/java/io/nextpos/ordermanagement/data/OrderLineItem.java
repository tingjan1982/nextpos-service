package io.nextpos.ordermanagement.data;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.shared.exception.BusinessLogicException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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

    private Date createdDate;

    private Date modifiedDate;


    public OrderLineItem(final ProductSnapshot productSnapshot, final int quantity, OrderSettings orderSettings) {
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;

        this.state = LineItemState.OPEN;
        this.subTotal = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());
        this.discountedSubTotal = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());

        computeSubTotal();

        this.createdDate = new Date();
        this.modifiedDate = new Date();
    }

    public TaxableAmount getProductPriceWithOptions() {

        final BigDecimal productTotal = productSnapshot.getProductPriceWithOptions();
        final TaxableAmount taxableProductPrice = subTotal.newInstance();
        taxableProductPrice.calculate(productTotal);

        return taxableProductPrice;
    }

    public void incrementQuantity() {
        this.quantity++;
        modifiedDate = new Date();

        computeSubTotal();
    }

    public void updateQuantityAndProductOptions(int quantity, BigDecimal overridePrice, List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots) {

        this.quantity = quantity;
        this.getProductSnapshot().setProductOptions(productOptionSnapshots);
        this.getProductSnapshot().setOverridePrice(overridePrice);
        modifiedDate = new Date();

        computeSubTotal();
    }

    /**
     * This method must be called whether line item is changed in the following ways:
     *  new line item is added.
     *  quantity is updated.
     *  product options is updated.
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

        if (discountedSubTotal.lessThanZero()) {
            throw new BusinessLogicException("message.discountedTotalLessThanZero", "Discounted amount cannot be less than zero");
        }

        modifiedDate = new Date();
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
        copy.discountedSubTotal = discountedSubTotal.copy();
        copy.appliedOfferInfo = appliedOfferInfo != null ? appliedOfferInfo.copy() : null;

        return copy;
    }

    public enum LineItemState {

        OPEN,

        IN_PROCESS,

        ALREADY_IN_PROCESS,

        DELIVERED
    }
}
