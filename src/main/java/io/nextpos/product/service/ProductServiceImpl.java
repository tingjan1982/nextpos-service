package io.nextpos.product.service;

import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@JpaTransaction
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductVersionRepository productVersionRepository;

    private final ProductSetRepository productSetRepository;

    @Autowired
    public ProductServiceImpl(final ProductRepository productRepository, final ProductVersionRepository productVersionRepository, final ProductSetRepository productSetRepository) {
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
        this.productSetRepository = productSetRepository;
    }

    @Override
    public Product saveProduct(final Product product) {
        return productRepository.save(product);
    }

    @Override
    public ProductSet saveProductSet(ProductSet productSet) {
        return productSetRepository.save(productSet);
    }

    @Override
    public Product getProduct(final String id) {
        return productRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Product.class);
        });
    }

    @Override
    public ProductSet getProductSet(final String id) {
        return productSetRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ProductSet.class);
        });
    }

    @Override
    public void deployProduct(final String id) {
        final Product product = this.getProduct(id);
        product.deploy();

        productVersionRepository.deleteRetiredProductVersions(product);

        productRepository.save(product);
    }

    @Override
    public void deleteProduct(final Product product) {
        productRepository.delete(product);
    }
}
