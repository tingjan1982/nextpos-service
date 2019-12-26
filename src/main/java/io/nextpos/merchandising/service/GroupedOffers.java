package io.nextpos.merchandising.service;

import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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

        final Comparator<Pair<ProductLevelOffer, BigDecimal>> discountComparator = Comparator.comparing(Pair::getRight);

        order.getOrderLineItems()
                .forEach(li -> productLevelOffers.stream()
                        .map(o -> {
                            final BigDecimal discount = o.calculateDiscount(li);
                            return Pair.of(o, discount);
                        })
                        .filter(p -> p.getRight().compareTo(BigDecimal.ZERO) > 0)
                        .min(discountComparator)
                        .ifPresent(p -> li.applyAndRecordOffer(p.getLeft(), p.getRight())));

        order.computeTotal();
    }

    void arbitrateBestOrderLevelOffer(Order order) {

        final Comparator<Pair<OrderLevelOffer, BigDecimal>> discountComparator = Comparator.comparing(Pair::getRight);

        orderLevelOffers.stream()
                .map(o -> {
                    final BigDecimal discount = o.calculateDiscount(order);
                    return Pair.of(o, discount);
                })
                .filter(p -> p.getRight().compareTo(BigDecimal.ZERO) > 0)
                .min(discountComparator)
                .ifPresent(p -> order.applyAndRecordOffer(p.getLeft(), p.getRight()));
    }

    public void addOrderLevelOffer(OrderLevelOffer orderLevelOffer) {
        orderLevelOffers.add(orderLevelOffer);
    }

    public void addProductLevelOffer(ProductLevelOffer productLevelOffer) {
        productLevelOffers.add(productLevelOffer);
    }
}
