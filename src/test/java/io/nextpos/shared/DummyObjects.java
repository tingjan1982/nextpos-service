package io.nextpos.shared;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.ProductVersion;

import java.math.BigDecimal;

public class DummyObjects {

    public static Client dummyClient() {
        return new Client("test", "admin@anextpos.io", "secret");
    }

    public static ProductVersion dummyProductVersion() {
        return new ProductVersion("name", "sku", "description", BigDecimal.ZERO);
    }
}
