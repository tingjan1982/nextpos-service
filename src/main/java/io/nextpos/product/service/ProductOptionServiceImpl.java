package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@JpaTransaction
public class ProductOptionServiceImpl implements ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    private final ProductOptionVersionRepository productOptionVersionRepository;

    private final ProductOptionRelationRepository<?> productOptionRelationRepository;

    @Autowired
    public ProductOptionServiceImpl(final ProductOptionRepository productOptionRepository, final ProductOptionVersionRepository productOptionVersionRepository, final ProductOptionRelationRepository productOptionRelationRepository) {
        this.productOptionRepository = productOptionRepository;
        this.productOptionVersionRepository = productOptionVersionRepository;
        this.productOptionRelationRepository = productOptionRelationRepository;
    }

    @Override
    public ProductOption saveProductOption(final ProductOption productOption) {
        return productOptionRepository.save(productOption);
    }

    @Override
    public ProductOption getProductOption(final String id) {
        return productOptionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ProductOption.class);
        });
    }

    @Override
    public List<? extends ProductOptionRelation> getProductOptionRelationsByProductOption(ProductOption productOption) {
        return productOptionRelationRepository.findAllByProductOption(productOption);
    }

    @Override
    public List<ProductOptionVersion> getProductOptions(final Client client, final Version version) {

        return productOptionVersionRepository.findAllByProductOptionClientAndVersionOrderByOptionName(client, version);
    }

    @Override
    public List<ProductOptionVersion> getProductOptionsByProductLabel(final Client client, final Version version, ProductLabel productLabel) {

        return productOptionVersionRepository.findByProductLabel(client, version, productLabel);
    }

    @Override
    public void deleteProductOption(ProductOption productOption) {

        final Long relationCount = productOptionRelationRepository.countByProductOption(productOption);

        if (relationCount > 0) {
            throw new BusinessLogicException("message.optionInUse", "Product option is in use: " + productOption.getId());
        }

        productOptionRepository.delete(productOption);
    }


    @Override
    public ProductOption deployProductOption(final String id) {

        final ProductOption productOption = getProductOption(id);
        productOption.deploy();

        productOptionVersionRepository.deleteRetiredProductOptionVersions(productOption);

        return productOptionRepository.save(productOption);
    }
}
