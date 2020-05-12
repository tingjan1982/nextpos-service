package io.nextpos.merchandising.data;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Entity(name = "client_product_level_offer")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductLevelOffer extends Offer implements DiscountCalculator<OrderLineItem> {

    private boolean appliesToAllProducts;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "client_product_level_offer_products",
            joinColumns = @JoinColumn(name = "offer_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    @Fetch(FetchMode.SUBSELECT)
    private List<Product> appliesToProducts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "client_product_level_offer_labels",
            joinColumns = @JoinColumn(name = "offer_id"),
            inverseJoinColumns = @JoinColumn(name = "product_label_id"))
    @Fetch(FetchMode.SUBSELECT)
    private List<ProductLabel> appliesToProductLabels = new ArrayList<>();


    public ProductLevelOffer(final Client client, final String name, final TriggerType triggerType, final DiscountType discountType, final BigDecimal discountValue, final boolean appliesToAllProducts) {
        super(client, name, triggerType, discountType, discountValue);
        this.appliesToAllProducts = appliesToAllProducts;
    }

    public void addProduct(Product product) {
        appliesToProducts.add(product);
    }

    public void addProductLabel(ProductLabel productLabel) {
        appliesToProductLabels.add(productLabel);
    }

    @Override
    public BigDecimal calculateDiscount(final OrderLineItem objectToDiscount) {

        final DiscountDetails discountDetails = this.getDiscountDetails();
        final BigDecimal discountValue = discountDetails.getDiscountValue();

        return this.calculateDiscount(objectToDiscount, discountValue);
    }

    @Override
    public BigDecimal calculateDiscount(OrderLineItem orderLineItem, final BigDecimal discountValue) {

        BigDecimal discountedPrice = BigDecimal.ZERO;
        final ProductSnapshot productSnapshot = orderLineItem.getProductSnapshot();

        if (isProductEligible(productSnapshot)) {
            return OfferDiscountUtils.calculateDiscount(orderLineItem.getProductPriceWithOptions(), this.getDiscountDetails());
        }

        return discountedPrice;
    }

    private boolean isProductEligible(ProductSnapshot product) {

        if (appliesToAllProducts) {
            return true;
        }

        if (appliesToProducts.stream().anyMatch(p -> StringUtils.equals(p.getId(), product.getId()))) {
            return true;
        }

        return appliesToProductLabels.stream().anyMatch(l -> StringUtils.equals(l.getId(), product.getLabelId()));
    }

    @Override
    public EnumSet<DiscountType> supportedDiscountType() {
        return EnumSet.allOf(DiscountType.class);
    }

    public enum GlobalProductDiscount {

        NO_DISCOUNT("No Discount", DiscountType.PERCENT_OFF, BigDecimal.valueOf(-1)),
        DISCOUNT_PERCENT_OFF("Discount %", DiscountType.PERCENT_OFF, BigDecimal.valueOf(0)),
        DISCOUNT_AMOUNT_OFF("Discount $", DiscountType.AMOUNT_OFF, BigDecimal.valueOf(0));

        private final String discountName;

        private final DiscountType discountType;

        private final BigDecimal discount;


        GlobalProductDiscount(final String discountName, final DiscountType discountType, final BigDecimal discount) {
            this.discountName = discountName;
            this.discountType = discountType;
            this.discount = discount;
        }

        public String getDiscountName() {
            return discountName;
        }

        public DiscountType getDiscountType() {
            return discountType;
        }

        public BigDecimal getDiscount() {
            return discount;
        }
    }
}
