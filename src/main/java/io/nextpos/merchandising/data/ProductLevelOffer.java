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
    public BigDecimal calculateDiscount(OrderLineItem orderLineItem) {

        BigDecimal discountedPrice = BigDecimal.ZERO;
        final ProductSnapshot productSnapshot = orderLineItem.getProductSnapshot();

        if (isProductEligible(productSnapshot)) {
            final BigDecimal productPrice = productSnapshot.getProductPriceWithOptions();

            final DiscountDetails discountDetails = this.getDiscountDetails();
            final BigDecimal discountValue = discountDetails.getDiscountValue();

            switch (discountDetails.getDiscountType()) {
                case AMOUNT:
                    discountedPrice = discountValue;
                    break;
                case AMOUNT_OFF:
                    discountedPrice = productPrice.subtract(discountValue);
                    break;
                case PERCENT_OFF:
                    final BigDecimal discount = productPrice.multiply(discountValue);
                    discountedPrice = productPrice.subtract(discount);
                    break;
            }
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
}