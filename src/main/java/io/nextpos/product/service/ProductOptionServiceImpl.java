package io.nextpos.product.service;

import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductOptionServiceImpl implements ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    private final ProductOptionVersionRepository productOptionVersionRepository;

    private final ProductOptionRelationRepository productOptionRelationRepository;

    @Autowired
    public ProductOptionServiceImpl(final ProductOptionRepository productOptionRepository, final ProductOptionVersionRepository productOptionVersionRepository, final ProductOptionRelationRepository productOptionRelationRepository) {
        this.productOptionRepository = productOptionRepository;
        this.productOptionVersionRepository = productOptionVersionRepository;
        this.productOptionRelationRepository = productOptionRelationRepository;
    }

    @Override
    public ProductOption createProductOption(final ProductOption productOption) {
        return productOptionRepository.save(productOption);
    }

    @Override
    public ProductOption getProductOption(final String id) {
        return productOptionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ProductOption.class);
        });
    }

    @Override
    public ProductOption deployProductOption(final String id) {

        final ProductOption productOption = getProductOption(id);
        productOption.deploy();

        productOptionVersionRepository.deleteRetiredProductOptionVersions(productOption);

        return productOptionRepository.save(productOption);
    }

    @Override
    public List<ProductOptionRelation> addProductOptionToProduct(final ProductOption productOption, final List<Product> products) {

        return products.stream()
                .map(p -> new ProductOptionRelation.ProductOptionOfProduct(productOption, p))
                .map(productOptionRelationRepository::save).collect(Collectors.toList());
    }

    // todo: revise add product option to product and label as the actual scenario would be many options to 1.
    /**
     * Creates a relationship between ProductOption and ProductLabel.
     * Also will apply all ProductOptions to all products that are associated with the ProductLabel.
     *
     * @param productOption
     * @param productLabels
     * @return
     */
    @Override
    public List<ProductOptionRelation> addProductOptionToProductLabel(final ProductOption productOption, final List<ProductLabel> productLabels) {

        return productLabels.stream()
                .map(l -> new ProductOptionRelation.ProductOptionOfLabel(productOption, l))
                .map(productOptionRelationRepository::save).collect(Collectors.toList());
    }
}
