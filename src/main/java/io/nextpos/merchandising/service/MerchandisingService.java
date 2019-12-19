package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;

import java.math.BigDecimal;

public interface MerchandisingService {

    Order computeOffers(Client client, Order order);

    Order applyOrderDiscount(Order order, BigDecimal discount);
}
