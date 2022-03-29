package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;

import java.math.BigDecimal;

public interface MerchandisingService {

    Order computeOffers(Client client, Order order);

    Order applyOrderOffer(Order order, String orderOfferId, BigDecimal overrideDiscountValue);

    Order applyFullDiscount(Order order);

    Order removeOrderOffer(Order order);

    OrderLineItem applyGlobalProductDiscount(OrderLineItem lineItem, ProductLevelOffer.GlobalProductDiscount globalProductDiscount, BigDecimal overrideDiscount);

    Order updateServiceCharge(Order order, boolean waiveServiceCharge);

    Order resetOrderOffers(Order order);
}
