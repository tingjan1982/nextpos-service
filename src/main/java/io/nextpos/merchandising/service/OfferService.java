package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;

import java.util.Map;

public interface OfferService {

    <T extends Offer> T saveOffer(T offer);

    Offer getOffer(String id);

    Offer activateOffer(Offer offer);

    Offer deactivateOffer(Offer offer);

    GroupedOffers findActiveOffers(Client client);

    OrderLevelOffer getGlobalOrderOffer(OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount);

    ProductLevelOffer getGlobalProductOffer(ProductLevelOffer.GlobalProductDiscount globalProductDiscount);

    Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> getGlobalOrderOffers();

    Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> getGlobalProductOffers();

    void deleteOffer(Offer offer);
}
