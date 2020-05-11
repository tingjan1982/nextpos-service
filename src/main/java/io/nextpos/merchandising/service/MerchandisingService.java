package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;

import java.math.BigDecimal;

public interface MerchandisingService {

    Order computeOffers(Client client, Order order);

    Order applyGlobalOrderDiscount(Order order, OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount, BigDecimal discount);

    OrderLineItem applyGlobalProductDiscount(OrderLineItem lineItem, ProductLevelOffer.GlobalProductDiscount globalProductDiscount, BigDecimal overrideDiscount);

    Order updateServiceCharge(Order order, boolean serviceCharge);

    Order resetOrderOffers(Order order);
}
