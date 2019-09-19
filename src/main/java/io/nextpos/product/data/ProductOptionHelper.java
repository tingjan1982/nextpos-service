package io.nextpos.product.data;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Helper class for Product and ProductLabel class to replace product options as their implementation is almost identical.
 */
class ProductOptionHelper {

    static <T extends ProductOptionRelation> void replaceProductOptions(List<T> optionRelations, Function<ProductOption, T> optionRelationProvider, ProductOption... productOptions) {

        optionRelations.forEach(ProductOptionRelation::unlinkAssociation);
        optionRelations.clear();

        if (productOptions != null) {
            Stream.of(productOptions).forEach(po -> {
                final T productOptionRelation = optionRelationProvider.apply(po);
                optionRelations.add(productOptionRelation);
            });
        }
    }
}
