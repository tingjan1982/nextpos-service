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

    private final ProductOptionRelationRepository productOptionRelationRepository;

    @Autowired
    public ProductOptionServiceImpl(final ProductOptionRepository productOptionRepository, final ProductOptionRelationRepository productOptionRelationRepository) {
        this.productOptionRepository = productOptionRepository;
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

        return productOptionRepository.save(productOption);
    }

    @Override
    public List<ProductOptionRelation> addProductOptionToProduct(final ProductOption productOption, final List<Product> products) {

        return products.stream()
                .map(p -> new ProductOptionRelation.ProductOptionOfProduct(productOption, p))
                .map(productOptionRelationRepository::save).collect(Collectors.toList());
    }
}
