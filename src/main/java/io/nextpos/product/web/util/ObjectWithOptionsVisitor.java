package io.nextpos.product.web.util;

import io.nextpos.product.data.ProductOption;

public interface ObjectWithOptionsVisitor<T> {

    void visit(T objectWithOptions, ProductOption... productOptions);
}
