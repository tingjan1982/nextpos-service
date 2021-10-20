package io.nextpos.merchandising.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferType;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.OfferService;
import io.nextpos.merchandising.web.model.OfferRequest;
import io.nextpos.merchandising.web.model.OfferResponse;
import io.nextpos.merchandising.web.model.OffersResponse;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offers")
public class OfferController {

    private final OfferService offerService;

    private final ProductService productService;

    private final ProductLabelService productLabelService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public OfferController(final OfferService offerService, final ProductService productService, final ProductLabelService productLabelService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.offerService = offerService;
        this.productService = productService;
        this.productLabelService = productLabelService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public OfferResponse createOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @Valid @RequestBody OfferRequest offerRequest) {

        Offer offer = fromOfferRequest(client, offerRequest);

        return toOfferResponse(offerService.saveOffer(offer));
    }

    private Offer fromOfferRequest(final Client client, final OfferRequest offerRequest) {

        Offer newOffer;

        switch (offerRequest.getOfferType()) {
            case ORDER:
                newOffer = new OrderLevelOffer(client,
                        offerRequest.getOfferName(),
                        offerRequest.getTriggerType(),
                        offerRequest.getDiscountType(),
                        offerRequest.getDiscountValue());
                break;

            case PRODUCT:
                final ProductLevelOffer productLevelOffer = new ProductLevelOffer(client,
                        offerRequest.getOfferName(),
                        offerRequest.getTriggerType(),
                        offerRequest.getDiscountType(),
                        offerRequest.getDiscountValue(),
                        offerRequest.isAppliesToAllProducts());

                updateApplicableProductsToOffer(productLevelOffer, offerRequest);

                newOffer = productLevelOffer;
                break;

            default:
                throw new GeneralApplicationException("Won't reach here");
        }

        newOffer.updateOfferEffectiveDate(offerRequest.getStartDate(), offerRequest.getEndDate());

        return newOffer;
    }

    @PostMapping("/{id}/activate")
    public OfferResponse activateOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                       @PathVariable final String id) {

        final Offer offer = clientObjectOwnershipService.checkOwnership(client, () -> offerService.getOffer(id));
        return toOfferResponse(offerService.activateOffer(offer));
    }

    @PostMapping("/{id}/deactivate")
    public OfferResponse deactivateOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @PathVariable final String id) {

        final Offer offer = clientObjectOwnershipService.checkOwnership(client, () -> offerService.getOffer(id));
        return toOfferResponse(offerService.deactivateOffer(offer));
    }

    @GetMapping
    public OffersResponse getOffers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        List<Offer> offers = offerService.getOffers(client);

        final List<OfferResponse> offerResponses = offers.stream().map(this::toOfferResponse).collect(Collectors.toList());
        return new OffersResponse(offerResponses);
    }

    @GetMapping("/{id}")
    public OfferResponse getOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                  @PathVariable final String id) {

        final Offer offer = clientObjectOwnershipService.checkOwnership(client, () -> offerService.getOffer(id));

        return toOfferResponse(offer);
    }

    @PostMapping("/{id}")
    public OfferResponse updateOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @PathVariable final String id,
                                     @Valid @RequestBody OfferRequest offerRequest) {

        final Offer offer = clientObjectOwnershipService.checkOwnership(client, () -> offerService.getOffer(id));

        updateOfferFromRequest(offer, offerRequest);

        return toOfferResponse(offerService.saveOffer(offer));
    }

    private void updateOfferFromRequest(final Offer offer, final OfferRequest offerRequest) {

        offer.setTriggerType(offerRequest.getTriggerType());
        offer.setName(offerRequest.getOfferName());
        offer.updateDiscountDetails(offerRequest.getDiscountType(), offerRequest.getDiscountValue());
        offer.updateOfferEffectiveDate(offerRequest.getStartDate(), offerRequest.getEndDate());

        if (offer instanceof ProductLevelOffer) {
            final ProductLevelOffer productLevelOffer = (ProductLevelOffer) offer;
            updateApplicableProductsToOffer(productLevelOffer, offerRequest);
        }
    }

    private void updateApplicableProductsToOffer(ProductLevelOffer productLevelOffer, OfferRequest request) {

        productLevelOffer.setAppliesToAllProducts(request.isAppliesToAllProducts());

        if (!productLevelOffer.isAppliesToAllProducts()) {
            productLevelOffer.getAppliesToProducts().clear();

            request.getProductIds().forEach(productId -> {
                final Product product = productService.getProduct(productId);
                productLevelOffer.addProduct(product);
            });

            productLevelOffer.getAppliesToProductLabels().clear();

            request.getProductLabelIds().forEach(labelId -> {
                final ProductLabel productLabel = productLabelService.getProductLabelOrThrows(labelId);
                productLevelOffer.addProductLabel(productLabel);
            });
        } else {
            productLevelOffer.getExcludedProducts().clear();
            request.getExcludedProductIds().forEach(pid -> {
                final Product product = productService.getProduct(pid);
                productLevelOffer.addExcludedProduct(product);
            });
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOffer(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                            @PathVariable final String id) {

        final Offer offer = clientObjectOwnershipService.checkOwnership(client, () -> offerService.getOffer(id));

        offerService.deleteOffer(offer);
    }

    @GetMapping({"/globalOrderOffers", "/orderOffers"})
    public OffersResponse getGlobalOrderOffers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<OfferResponse> offers = offerService.getGlobalOrderOffers().values().stream()
                .map(this::toOfferResponse).collect(Collectors.toList());

        offerService.getActiveOffers(client, OfferType.ORDER, Offer.TriggerType.AT_CHECKOUT).stream()
                .map(this::toOfferResponse).forEach(offers::add);

        return new OffersResponse(offers);
    }

    @GetMapping({"/globalProductOffers", "/productOffers"})
    public OffersResponse getGlobalProductOffers() {

        final List<OfferResponse> offers = offerService.getGlobalProductOffers().values().stream()
                .map(this::toOfferResponse).collect(Collectors.toList());

        return new OffersResponse(offers);
    }

    private OfferResponse toOfferResponse(Offer offer) {
        final OfferType offerType = offer instanceof OrderLevelOffer ? OfferType.ORDER : OfferType.PRODUCT;

        final OfferResponse offerResponse = new OfferResponse(
                offer.getId(),
                offerType,
                offer.getName(),
                offer.getName(),
                offer.getTriggerType(),
                offer.getDiscountDetails().getDiscountType(),
                offer.getDiscountDetails().getDiscountValue(),
                offer.getEffectiveDetails().isActive(),
                offer.getEffectiveDetails().getStartDate(),
                offer.getEffectiveDetails().getEndDate());

        if (offerType == OfferType.PRODUCT) {
            final ProductLevelOffer productOffer = (ProductLevelOffer) offer;
            final Map<String, String> productIds = productOffer.getAppliesToProducts().stream()
                    .collect(Collectors.toMap(Product::getId, p -> p.getDesignVersion().getProductName()));
            final Map<String, String> excludedProductIds = productOffer.getExcludedProducts().stream()
                    .collect(Collectors.toMap(Product::getId, p -> p.getDesignVersion().getProductName()));
            final Map<String, String> productLabelIds = productOffer.getAppliesToProductLabels().stream()
                    .collect(Collectors.toMap(ProductLabel::getId, ProductLabel::getName));

            final List<OfferResponse.ProductOfferProduct> selectedProducts = productOffer.getAppliesToProducts().stream()
                    .map(p -> new OfferResponse.ProductOfferProduct(
                            p.getProductLabel() != null ? p.getProductLabel().getId() : null,
                            p.getId(),
                            p.getDesignVersion().getProductName()
                    )).collect(Collectors.toList());

            final List<OfferResponse.ProductOfferProduct> selectedExcludedProducts = productOffer.getExcludedProducts().stream()
                    .map(p -> new OfferResponse.ProductOfferProduct(
                            p.getProductLabel() != null ? p.getProductLabel().getId() : null,
                            p.getId(),
                            p.getDesignVersion().getProductName()
                    )).collect(Collectors.toList());

            final OfferResponse.ProductOfferDetails productOfferDetails = new OfferResponse.ProductOfferDetails(productOffer.isAppliesToAllProducts(),
                    productIds,
                    excludedProductIds,
                    productLabelIds,
                    selectedProducts,
                    selectedExcludedProducts);

            offerResponse.setProductOfferDetails(productOfferDetails);
        }

        return offerResponse;
    }
}
