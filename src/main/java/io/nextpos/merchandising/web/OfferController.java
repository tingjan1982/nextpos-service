package io.nextpos.merchandising.web;

import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.OfferService;
import io.nextpos.merchandising.web.model.OfferResponse;
import io.nextpos.merchandising.web.model.OffersResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(final OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/globalOrderOffers")
    public OffersResponse getGlobalOrderOffers() {

        final List<OfferResponse> offers = offerService.getGlobalOrderOffers().entrySet().stream()
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

    @GetMapping("/globalProductOffers")
    public OffersResponse getGlobalProductOffers() {

        final List<OfferResponse> offers = offerService.getGlobalProductOffers().entrySet().stream()
                .map(entry -> {
                    final ProductLevelOffer offer = entry.getValue();
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
