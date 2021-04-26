package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@JpaTransaction
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;

    private final OrderOfferRepository orderOfferRepository;

    private final ProductOfferRepository productOfferRepository;

    private final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers;

    private final Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers;

    @Autowired
    public OfferServiceImpl(final OfferRepository offerRepository, final OrderOfferRepository orderOfferRepository, final ProductOfferRepository productOfferRepository, final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers, final Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers) {
        this.offerRepository = offerRepository;
        this.orderOfferRepository = orderOfferRepository;
        this.productOfferRepository = productOfferRepository;
        this.globalOrderLevelOffers = globalOrderLevelOffers;
        this.globalProductLevelOffers = globalProductLevelOffers;
    }

    @Override
    public <T extends Offer> T saveOffer(final T offer) {
        return offerRepository.save(offer);
    }

    @Override
    public Offer getOffer(final String id) {
        return offerRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Offer.class);
        });
    }

    @Override
    public OrderLevelOffer resolveOrderOffer(String orderOfferId) {
        return orderOfferRepository.findById(orderOfferId).orElseGet(() -> {
            try {
                final OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount = OrderLevelOffer.GlobalOrderDiscount.valueOf(orderOfferId);
                return this.getGlobalOrderOffer(globalOrderDiscount);

            } catch (Exception e) {
                throw new BusinessLogicException("message.invalidOfferId", "Invalid offer Id: " + orderOfferId);
            }
        });
    }

    @Override
    public Offer activateOffer(Offer offer) {
        offer.updateOfferActiveStatus(true);
        return offerRepository.save(offer);
    }

    @Override
    public Offer deactivateOffer(Offer offer) {
        offer.updateOfferActiveStatus(false);
        return offerRepository.save(offer);
    }

    @Override
    public List<Offer> getOffers(final Client client) {
        return offerRepository.findAllByClientOrderByName(client);
    }

    @Override
    public List<? extends Offer> getActiveOffers(final Client client, final OfferType offerType, final Offer.TriggerType triggerType) {

        switch(offerType) {
            case ORDER:
                return orderOfferRepository.findActiveOffers(client, triggerType, new Date());
            case PRODUCT:
                return productOfferRepository.findActiveOffers(client, triggerType, new Date());
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public GroupedOffers findActiveOffers(Client client) {

        final GroupedOffers groupedOffers = new GroupedOffers();
        final Stream<Offer> offersStream = offerRepository.findActiveOffers(client, Offer.TriggerType.ALWAYS, new Date());

        offersStream.forEach(o -> {
            if (o instanceof OrderLevelOffer) {
                groupedOffers.addOrderLevelOffer((OrderLevelOffer) o);

            } else if (o instanceof ProductLevelOffer) {
                groupedOffers.addProductLevelOffer((ProductLevelOffer) o);
            }
        });

        return groupedOffers;
    }

    @Override
    public OrderLevelOffer getGlobalOrderOffer(OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount) {
        return globalOrderLevelOffers.get(globalOrderDiscount);
    }

    @Override
    public ProductLevelOffer getGlobalProductOffer(ProductLevelOffer.GlobalProductDiscount globalProductDiscount) {
        return globalProductLevelOffers.get(globalProductDiscount);
    }

    @Override
    public Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> getGlobalOrderOffers() {
        return globalOrderLevelOffers;
    }

    @Override
    public Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> getGlobalProductOffers() {
        return globalProductLevelOffers;
    }

    @Override
    public void deleteOffer(final Offer offer) {

        offerRepository.delete(offer);
    }
}
