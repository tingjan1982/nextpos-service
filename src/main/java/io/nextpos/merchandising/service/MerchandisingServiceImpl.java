package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
@Transactional
public class MerchandisingServiceImpl implements MerchandisingService {

    private final OfferService offerService;

    private final OrderRepository orderRepository;

    @Autowired
    public MerchandisingServiceImpl(final OfferService offerService, final OrderRepository orderRepository) {
        this.offerService = offerService;
        this.orderRepository = orderRepository;
    }

    /**
     * Order of offer application (stackable, (best one applies if there are overlaps in each step):
     * <p>
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

        return orderRepository.save(order);
    }

    @Override
    public Order applyGlobalOrderDiscount(final Order order, OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount, BigDecimal overrideDiscount) {

        if (globalOrderDiscount == OrderLevelOffer.GlobalOrderDiscount.NO_DISCOUNT) {
            order.removeOffer();

        } else {
            final OrderLevelOffer globalOffer = offerService.getGlobalOrderOffer(globalOrderDiscount);
            BigDecimal computedDiscount;

            if (globalOffer.isZeroDiscount()) {
                computedDiscount = globalOffer.calculateDiscount(order, overrideDiscount);
            } else {
                computedDiscount = globalOffer.calculateDiscount(order);
            }

            order.applyAndRecordOffer(globalOffer, computedDiscount, overrideDiscount);
        }

        return orderRepository.save(order);
    }

    @Override
    public OrderLineItem applyGlobalProductDiscount(OrderLineItem lineItem, ProductLevelOffer.GlobalProductDiscount globalProductDiscount, BigDecimal overrideDiscount) {

        final ProductLevelOffer globalProductOffer = offerService.getGlobalProductOffer(globalProductDiscount);
        BigDecimal computedDiscount;

        if (globalProductOffer.isZeroDiscount()) {
            computedDiscount = globalProductOffer.calculateDiscount(lineItem, overrideDiscount);
        } else {
            computedDiscount = globalProductOffer.calculateDiscount(lineItem);
        }

        lineItem.applyAndRecordOffer(globalProductOffer, computedDiscount, overrideDiscount);

        return lineItem;
    }

    @Override
    public Order updateServiceCharge(final Order order, final boolean waiveServiceCharge) {

        BigDecimal serviceCharge = BigDecimal.ZERO;

        if (!waiveServiceCharge) {
            final OrderSettings originalOrderSettings = (OrderSettings) order.getMetadata(Order.ORIGINAL_ORDER_SETTINGS);
            if (originalOrderSettings != null) {
                serviceCharge = originalOrderSettings.getServiceCharge();
            }
        }

        order.updateServiceCharge(serviceCharge);
        return orderRepository.save(order);
    }

    @Override
    public Order resetOrderOffers(Order order) {

        Order.OperationPipeline.executeAfter(order, () -> {
            order.removeOffer();

            final OrderSettings originalOrderSettings = (OrderSettings) order.getMetadata(Order.ORIGINAL_ORDER_SETTINGS);

            if (originalOrderSettings != null) {
                order.updateServiceCharge(originalOrderSettings.getServiceCharge());
            }
        });

        return orderRepository.save(order);
    }

    /**
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
