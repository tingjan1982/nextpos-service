package io.nextpos.product.service;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.shared.event.SimpleSaveEvent;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ProductServiceImpl(final ProductRepository productRepository, final ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }


    @Override
    public Product createProduct(final Product product) {
        eventPublisher.publishEvent(new SimpleSaveEvent(product));

        return productRepository.save(product);
    }

    @Override
    public Product getProduct(final String id) {
        final Optional<Product> productOptional = productRepository.findById(id);

        return productOptional.orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Product.class);
        });
    }

    @Override
    public void deleteProduct(final Product product) {

    }
}
