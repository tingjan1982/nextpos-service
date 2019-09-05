package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;

public interface MerchandisingService {

    Order computeOffers(Client client, Order order);
}
