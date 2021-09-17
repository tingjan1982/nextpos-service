package io.nextpos.product.data;

public enum ProductType {

    PRODUCT,
    PRODUCT_SET,
    PRODUCT_COMBO,
    PARENT_PRODUCT;

    public static ProductType resolveProductType(Product product) {

        if (product instanceof ProductCombo) {
            return PRODUCT_COMBO;
        } else if (product instanceof ProductSet) {
            return PRODUCT_SET;
        } else {
            return PRODUCT;
        }
    }
}
