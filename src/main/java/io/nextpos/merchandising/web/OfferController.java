package io.nextpos.merchandising.web;

import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.web.model.OfferResponse;
import io.nextpos.merchandising.web.model.OffersResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offers")
public class OfferController {

    private final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers;

    public OfferController(final Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers) {
        this.globalOrderLevelOffers = globalOrderLevelOffers;
    }

    @GetMapping("/globalOrderOffers")
    public OffersResponse getGlobalOrderOffers() {

        final List<OfferResponse> offers = globalOrderLevelOffers.entrySet().stream()
                .map(entry -> {
                    final OrderLevelOffer offer = entry.getValue();
                    return new OfferResponse(
                            offer.getId(),
                            offer.getName(),
                            entry.getKey().getDiscountName(),
                            offer.getTriggerType(),
                            offer.getDiscountDetails().getDiscountType(),
                            offer.getDiscountDetails().getDiscountValue().multiply(BigDecimal.valueOf(100)));
                }).collect(Collectors.toList());

        return new OffersResponse(offers);
    }
}
