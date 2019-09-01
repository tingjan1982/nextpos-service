package io.nextpos.product.web.util;

import java.util.List;

public interface ObjectWithProductOptions {

    <T> void accept(final T objectWithOptions, ObjectWithOptionsVisitor<T> objectWithOptionsVisitor, List<String> productOptionIds);
}
