package io.nextpos.product.data;

import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void getObjectByVersionThrows() {

        final Product product = new Product(DummyObjects.dummyClient(), DummyObjects.dummyProductVersion());
        assertThat(product.getObjectVersioningClassType()).isEqualTo(ProductVersion.class);

        assertDoesNotThrow(product::getDesignVersion);
        assertThrows(ObjectNotFoundException.class, product::getLiveVersion);

        product.deploy();

        assertDoesNotThrow(product::getLiveVersion);
    }
}