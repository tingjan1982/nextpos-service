package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferType;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;

import java.util.List;
import java.util.Map;

public interface OfferService {

    <T extends Offer> T saveOffer(T offer);

    Offer getOffer(String id);

    OrderLevelOffer resolveOrderOffer(String orderOfferId);

    Offer activateOffer(Offer offer);

    Offer deactivateOffer(Offer offer);

    List<Offer> getOffers(Client client);

    List<? extends Offer> getActiveOffers(Client client, OfferType offerType, Offer.TriggerType triggerType);

    GroupedOffers findActiveOffers(Client client);

    OrderLevelOffer getGlobalOrderOffer(OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount);

    ProductLevelOffer getGlobalProductOffer(ProductLevelOffer.GlobalProductDiscount globalProductDiscount);

    Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> getGlobalOrderOffers();

    Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> getGlobalProductOffers();

    void deleteOffer(Offer offer);
}
