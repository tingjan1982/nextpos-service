package io.nextpos.product.web.util;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.product.service.ProductOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Aims to simplify the logic of product and product labels trying to replace its product options using Visitor design pattern.
 */
@Component
public class ObjectWithProductOptionVisitorWrapper implements ObjectWithProductOptions {

    private final ProductOptionService productOptionService;

    private final ProductVisitor productVisitor;

    private final ProductLabelVisitor productLabelVisitor;

    @Autowired
    public ObjectWithProductOptionVisitorWrapper(final ProductOptionService productOptionService, final ProductVisitor productVisitor, final ProductLabelVisitor productLabelVisitor) {
        this.productOptionService = productOptionService;
        this.productVisitor = productVisitor;
        this.productLabelVisitor = productLabelVisitor;
    }

    public void accept(Product product, final List<String> productOptionIds) {
        this.accept(product, productVisitor, productOptionIds);
    }

    public void accept(ProductLabel productLabel, final List<String> productOptionIds) {
        this.accept(productLabel, productLabelVisitor, productOptionIds);
    }

    @Override
    public <T> void accept(final T objectWithOptions, final ObjectWithOptionsVisitor<T> objectWithOptionsVisitor, final List<String> productOptionIds) {

        if (!CollectionUtils.isEmpty(productOptionIds)) {
            final ProductOption[] resolvedProductOptions = productOptionIds.stream()
                    .map(productOptionService::getProductOption).toArray(ProductOption[]::new);

            objectWithOptionsVisitor.visit(objectWithOptions, resolvedProductOptions);
        } else {
            objectWithOptionsVisitor.visit(objectWithOptions);
        }
    }

    @Component
    public static class ProductVisitor implements ObjectWithOptionsVisitor<Product> {

        @Override
        public void visit(final Product objectWithOptions, final ProductOption... productOptions) {
            objectWithOptions.replaceProductOptions(productOptions);
        }
    }

    @Component
    public static class ProductLabelVisitor implements ObjectWithOptionsVisitor<ProductLabel> {

        @Override
        public void visit(final ProductLabel objectWithOptions, final ProductOption... productOptions) {
            objectWithOptions.replaceProductOptions(productOptions);
        }
    }
}
