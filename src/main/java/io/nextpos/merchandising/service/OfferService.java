package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;

public interface OfferService {

    <T extends Offer> T saveOffer(T offer);

    Offer getOffer(String id);

    Offer activateOffer(Offer offer);

    Offer deactivateOffer(Offer offer);

    GroupedOffers findActiveOffers(Client client);

    OrderLevelOffer getGlobalOfferByName(OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount);
}
