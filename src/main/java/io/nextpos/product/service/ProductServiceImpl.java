package io.nextpos.product.service;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.product.data.ProductVersionRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductVersionRepository productVersionRepository;

    @Autowired
    public ProductServiceImpl(final ProductRepository productRepository, final ProductVersionRepository productVersionRepository) {
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
    }

    @Override
    public Product saveProduct(final Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product getProduct(final String id) {
        return productRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Product.class);
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
