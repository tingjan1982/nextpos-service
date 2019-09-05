package io.nextpos.merchandising.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class GroupedOffers {

    private List<OrderLevelOffer> orderLevelOffers = new ArrayList<>();

    private List<ProductLevelOffer> productLevelOffers = new ArrayList<>();


    public void arbitrateBestProductLevelOffer(Order order) {

        order.getOrderLineItems()
                .forEach(li -> productLevelOffers.stream()
                        .map(o -> o.calculateDiscount(li))
                        .filter(d -> d.compareTo(BigDecimal.ZERO) > 0)
                        .min(BigDecimal::compareTo).ifPresent(li::computeDiscountedSubTotal));

        order.computeTotal();
    }

    public void arbitrateBestOrderLevelOffer(Order order) {

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
