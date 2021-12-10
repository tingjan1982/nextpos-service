package io.nextpos.ordermanagement.data;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.shared.exception.BusinessLogicException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    private BigDecimal lineItemSubTotal = BigDecimal.ZERO;

    private BigDecimal comboTotal = BigDecimal.ZERO;

    private AppliedOfferInfo appliedOfferInfo;

    private String associatedLineItemId;

    private transient List<OrderLineItem> childLineItems = new ArrayList<>();

    private Date createdDate;

    private Date modifiedDate;

    /**
     * The position which to display this line item on screen.
     */
    private int order;

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

    public String getProductOptions() {
        return this.getProductSnapshot().getProductOptions().stream()
                .map(po -> {
                    final StringBuilder option = new StringBuilder(po.getOptionValue());

                    if (po.getOptionPrice().compareTo(BigDecimal.ZERO) != 0) {
                        option.append("$").append(po.getOptionPrice());
                    }

                    return option.toString();
                })
                .collect(Collectors.joining(", "));
    }

    public TaxableAmount getProductPriceWithOptions() {

        final BigDecimal productTotal = productSnapshot.getProductPriceWithOptions();
        final TaxableAmount taxableProductPrice = subTotal.newInstance();
        taxableProductPrice.calculate(productTotal);

        return taxableProductPrice;
    }

    public void incrementQuantity(int quantityToAdd) {
        this.performOperation(li -> li.quantity += quantityToAdd);
    }

    public void decrementQuantity(int quantityToSubtract) {
        this.performOperation(li -> li.quantity -= quantityToSubtract);
    }

    public void updateQuantityAndProductOptions(int quantity, BigDecimal overridePrice, List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots) {
        this.performOperation(li -> {
            this.quantity = quantity;
            this.getProductSnapshot().setProductOptions(productOptionSnapshots);
            this.getProductSnapshot().setOverridePrice(overridePrice);
        });
    }

    /**
     * This is designed to perform operations on this line item and associated nested classes
     * and invoke compute subtotal.
     */
    public void performOperation(Consumer<OrderLineItem> updateOperation) {
        Assert.notNull(updateOperation, "Update operation cannot be empty");

        updateOperation.accept(this);

        modifiedDate = new Date();
        computeSubTotal();
    }

    /**
     * This method must be called whether line item is changed in the following ways:
     * new line item is added.
     * quantity is updated.
     * product options is updated.
     */
    public void computeSubTotal() {

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
            this.throwDiscountLessThanZeroException();
        }

        modifiedDate = new Date();

        final TaxableAmount deducedSubTotal = getDeducedSubTotal();
        lineItemSubTotal = deducedSubTotal.getAmount();
    }

    public TaxableAmount getDeducedSubTotal() {
        return discountedSubTotal != null && !discountedSubTotal.isZero() ? discountedSubTotal : this.subTotal;
    }

    /**
     * For tax inclusive scenarios, return amountWithTax because tax will not be calculated again, whereas in tax exclusive scenarios,
     * return amountWithoutTax so an Order will aggregate and calculate tax as a whole.
     *
     * @return
     */
//    public BigDecimal getLineItemSubTotal() {
//
//        final TaxableAmount subTotal = discountedSubTotal != null && !discountedSubTotal.isZero() ? discountedSubTotal : this.subTotal;
//        return subTotal.getAmount();
//    }
    public OrderLineItem splitCopy() {

        final OrderLineItem lineItem = this.copy();
        lineItem.id = this.id; // use the same id as source to be able to move quantity between source and target order.
        lineItem.performOperation(li -> li.quantity = 1);

        return lineItem;
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
        copy.lineItemSubTotal = lineItemSubTotal;
        copy.comboTotal = comboTotal;
        copy.associatedLineItemId = associatedLineItemId;
        copy.childLineItems = childLineItems.stream().map(OrderLineItem::copy).collect(Collectors.toList());

        copy.createdDate = new Date();
        copy.modifiedDate = new Date();

        return copy;
    }

    public boolean hasChildLineItems() {
        return !childLineItems.isEmpty();
    }

    public enum LineItemState {

        OPEN(false),

        IN_PROCESS(true),

        ALREADY_IN_PROCESS(true),

        PREPARED(false),

        DELIVERED(false),

        DELETED(false);

        private final boolean preparing;

        LineItemState(boolean preparing) {
            this.preparing = preparing;
        }

        public boolean isPreparing() {
            return preparing;
        }
    }
}
