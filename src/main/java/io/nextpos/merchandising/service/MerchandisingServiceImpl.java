package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
@Transactional
public class MerchandisingServiceImpl implements MerchandisingService {

    private final OfferService offerService;

    private final OrderService orderService;

    @Autowired
    public MerchandisingServiceImpl(final OfferService offerService, final OrderService orderService) {
        this.offerService = offerService;
        this.orderService = orderService;
    }

    /**
     * Order of offer application (stackable, (best one applies if there are overlaps in each step):
     *
     * 1. always triggered product and label level offers
     * 2. always triggered order level offer
     *
     * @param order
     * @return
     */
    @Override
    public Order computeOffers(final Client client, final Order order) {

        final GroupedOffers activeOffers = offerService.findActiveOffers(client);
        activeOffers.arbitrateBestProductLevelOffer(order);
        activeOffers.arbitrateBestOrderLevelOffer(order);

        return orderService.saveOrder(order);
    }

    @Override
    public Order applyGlobalOrderDiscount(final Order order, OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount, BigDecimal overrideDiscount) {

        final OrderLevelOffer globalOffer = offerService.getGlobalOfferByName(globalOrderDiscount);

        BigDecimal computedDiscount;
        
        if (globalOffer.isZeroDiscount()) {
            computedDiscount = globalOffer.calculateDiscount(order, overrideDiscount);
        } else {
            computedDiscount = globalOffer.calculateDiscount(order);
        }

        order.applyAndRecordOffer(globalOffer, computedDiscount);

        return orderService.saveOrder(order);
    }

    /**
     *
     *
     * 3. member triggered product and label level offers
     * 4. member triggered order level offer
     *
     * @param membership
     * @return
     */
    public Order triggerMemberOffers(String membership) {

        return null;
    }
}
