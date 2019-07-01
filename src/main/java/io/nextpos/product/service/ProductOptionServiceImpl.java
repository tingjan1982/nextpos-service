package io.nextpos.product.service;

import io.nextpos.product.data.ProductOption;
import io.nextpos.product.data.ProductOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class ProductOptionServiceImpl implements ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    @Autowired
    public ProductOptionServiceImpl(final ProductOptionRepository productOptionRepository) {
        this.productOptionRepository = productOptionRepository;
    }

    @Override
    public ProductOption createProductOption(final ProductOption productOption) {
        return productOptionRepository.save(productOption);
    }

    @Override
    public ProductOption getProductOption(final String id) {
        return productOptionRepository.getOne(id);
    }
}
