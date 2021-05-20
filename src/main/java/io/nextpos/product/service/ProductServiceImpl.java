package io.nextpos.product.service;

import io.nextpos.product.data.*;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@JpaTransaction
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductVersionRepository productVersionRepository;

    private final ProductSetRepository productSetRepository;

    private final ParentProductRepository parentProductRepository;

    private final VariationDefinitionRepository variationDefinitionRepository;

    @Autowired
    public ProductServiceImpl(final ProductRepository productRepository, final ProductVersionRepository productVersionRepository, final ProductSetRepository productSetRepository, ParentProductRepository parentProductRepository, VariationDefinitionRepository variationDefinitionRepository) {
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
        this.productSetRepository = productSetRepository;
        this.parentProductRepository = parentProductRepository;
        this.variationDefinitionRepository = variationDefinitionRepository;
    }

    @Override
    public Product saveProduct(final Product product) {

        product.updateSettingsFromProductLabel();
        return productRepository.save(product);
    }

    @Override
    public ProductSet saveProductSet(ProductSet productSet) {
        return productSetRepository.save(productSet);
    }

    @Override
    public ParentProduct saveParentProduct(ParentProduct parentProduct) {
        return parentProductRepository.save(parentProduct);
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
    public ParentProduct getParentProduct(String id) {
        return parentProductRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ParentProduct.class);
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
    public void reorderProducts(List<String> productIds) {

        final AtomicInteger order = new AtomicInteger(1);

        productIds.forEach(productId -> productRepository.findById(productId).ifPresent(p -> {
            p.setOrdering(order.getAndIncrement());
            this.saveProduct(p);
        }));
    }

    @Override
    public void deleteProduct(final Product product) {
        productRepository.delete(product);
    }

    @Override
    public VariationDefinition saveVariationDefinition(VariationDefinition variationDefinition) {
        return variationDefinitionRepository.save(variationDefinition);
    }

    @Override
    public VariationDefinition getVariationDefinition(String id) {
        return variationDefinitionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, VariationDefinition.class);
        });
    }
}
