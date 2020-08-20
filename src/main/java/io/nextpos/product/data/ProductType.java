package io.nextpos.product.data;

public enum ProductType {

    PRODUCT,
    PRODUCT_SET,
    PRODUCT_WITH_VARIATIONS;

    public static ProductType resolveProductType(Product product) {

        if (product instanceof ProductSet) {
            return PRODUCT_SET;
        } else {
            return PRODUCT;
        }
    }
}
