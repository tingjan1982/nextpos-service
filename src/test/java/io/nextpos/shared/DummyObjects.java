package io.nextpos.shared;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.product.data.ProductVersion;

import java.math.BigDecimal;
import java.util.Collections;

public class DummyObjects {

    public static Client dummyClient() {
        return new Client("test", "admin@nextpos.io", "secret", "TW");
    }

    public static ClientUser dummyClientUser() {
        return new ClientUser(new ClientUser.ClientUserId("test-user", "admin@nextpos.io"), "password", "ADMIN,USER");
    }

    public static ProductVersion dummyProductVersion() {
        return dummyProductVersion("name");
    }

    public static ProductVersion dummyProductVersion(String productName) {
        return new ProductVersion(productName, "sku", "description", BigDecimal.ZERO);
    }

    public static ProductSnapshot productSnapshot() {
        return new ProductSnapshot("pid", "coffee", "sku", BigDecimal.valueOf(100), Collections.emptyList());
    }
}
