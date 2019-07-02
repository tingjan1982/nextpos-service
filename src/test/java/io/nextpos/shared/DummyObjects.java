package io.nextpos.shared;

import io.nextpos.product.data.ProductVersion;

import java.math.BigDecimal;

public class DummyObjects {


    public static ProductVersion dummyProductVersion() {
        return new ProductVersion("name", "sku", "description", BigDecimal.ZERO);
    }
}
