package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class ProductOptionServiceImpl implements ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    private final ProductOptionVersionRepository productOptionVersionRepository;

    @Autowired
    public ProductOptionServiceImpl(final ProductOptionRepository productOptionRepository, final ProductOptionVersionRepository productOptionVersionRepository) {
        this.productOptionRepository = productOptionRepository;
        this.productOptionVersionRepository = productOptionVersionRepository;
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
    public List<ProductOptionVersion> getProductOptions(final Client client, final Version version) {

        return productOptionVersionRepository.findAllByProductOptionClientAndVersionOrderByOptionName(client, version);
    }

    @Override
    public List<ProductOptionVersion> getProductOptionsByProductLabel(final Client client, final Version version, ProductLabel productLabel) {

        return productOptionVersionRepository.findByProductLabel(client, version, productLabel);
    }

    @Override
    public ProductOption deployProductOption(final String id) {

        final ProductOption productOption = getProductOption(id);
        productOption.deploy();

        productOptionVersionRepository.deleteRetiredProductOptionVersions(productOption);

        return productOptionRepository.save(productOption);
    }
}
