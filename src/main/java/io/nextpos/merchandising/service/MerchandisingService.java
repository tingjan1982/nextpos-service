package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.ordermanagement.data.Order;

import java.math.BigDecimal;

public interface MerchandisingService {

    Order computeOffers(Client client, Order order);

    Order applyGlobalOrderDiscount(Order order, OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount, BigDecimal discount);

    Order updateServiceCharge(Order order, boolean serviceCharge);

    Order resetOrderOffers(Order order);
}
