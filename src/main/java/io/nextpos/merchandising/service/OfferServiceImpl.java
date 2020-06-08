package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferRepository;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;

    private final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers;

    private final Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers;

    @Autowired
    public OfferServiceImpl(final OfferRepository offerRepository, final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers, final Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers) {
        this.offerRepository = offerRepository;
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
    public Offer activateOffer(Offer offer) {
        offer.updateOfferEffectiveDetails(true);
        return offerRepository.save(offer);
    }

    @Override
    public Offer deactivateOffer(Offer offer) {
        offer.updateOfferEffectiveDetails(false);
        return offerRepository.save(offer);
    }

    @Override
    public List<Offer> getOffers(final Client client) {
        return offerRepository.findAllByClientOrderByName(client);
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
