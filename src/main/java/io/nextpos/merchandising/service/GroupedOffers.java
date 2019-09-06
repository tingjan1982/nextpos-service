package io.nextpos.merchandising.service;

import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is meant to used by MerchandisingService which is a public facing service to properly orchestrate
 * merchandising behavior. Hence, this class is designed to have package access.
 */
@Data
class GroupedOffers {

    private List<OrderLevelOffer> orderLevelOffers = new ArrayList<>();

    private List<ProductLevelOffer> productLevelOffers = new ArrayList<>();


    void arbitrateBestProductLevelOffer(Order order) {

        order.getOrderLineItems()
                .forEach(li -> productLevelOffers.stream()
                        .map(o -> o.calculateDiscount(li))
                        .filter(d -> d.compareTo(BigDecimal.ZERO) > 0)
                        .min(BigDecimal::compareTo).ifPresent(li::computeDiscountedSubTotal));

        order.computeTotal();
    }

    void arbitrateBestOrderLevelOffer(Order order) {

        orderLevelOffers.stream()
                .map(o -> o.calculateDiscount(order))
                .filter(d -> d.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo).ifPresent(order::applyDiscountedTotal);
    }

    public void addOrderLevelOffer(OrderLevelOffer orderLevelOffer) {
        orderLevelOffers.add(orderLevelOffer);
    }

    public void addProductLevelOffer(ProductLevelOffer productLevelOffer) {
        productLevelOffers.add(productLevelOffer);
    }
}
