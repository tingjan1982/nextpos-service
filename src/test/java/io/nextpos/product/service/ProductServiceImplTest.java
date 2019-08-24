package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.product.data.*;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProductServiceImplTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ProductVersionRepository productVersionRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Client createdClient;

    @BeforeEach
    public void prepare() {
        final Client client = DummyObjects.dummyClient();
        createdClient = clientRepository.save(client);
    }

    @Test
    void crudProduct() {

        final ProductLabel label = new ProductLabel("label", createdClient);
        productLabelService.createProductLabel(label);

        final ProductVersion productVersion = new ProductVersion("Gin Topic", "sku-001", "signature drink", BigDecimal.valueOf(350));
        final Product product = new Product(createdClient, productVersion);
        product.setProductLabel(label);

        final Product createdProduct = productService.saveProduct(product);

        assertProduct(createdProduct, product);

        final Product existingProduct = productService.getProduct(createdProduct.getId());
        assertProduct(existingProduct, product);

        existingProduct.setProductLabel(null);
        existingProduct.getDesignVersion().setProductName("updated");
        productService.saveProduct(existingProduct);

        final Product updatedProduct = productService.getProduct(existingProduct.getId());
        assertThat(updatedProduct.getId()).isEqualTo(createdProduct.getId());
        assertThat(updatedProduct.getProductLabel()).isNull();
        assertThat(updatedProduct.getDesignVersion().getProductName()).isEqualTo("updated");

        productService.deleteProduct(updatedProduct);

        assertThrows(ObjectNotFoundException.class, () -> productService.getProduct(updatedProduct.getId()));
    }

    private void assertProduct(Product actual, Product expected) {

        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getClient().getId()).isNotNull();
        assertThat(actual.getProductLabel()).isNotNull();
        assertThat(actual.getDesignVersion().getProductName()).isEqualTo(expected.getDesignVersion().getProductName());
        assertThat(actual.getDesignVersion().getSku()).isEqualTo(expected.getDesignVersion().getSku());
        assertThat(actual.getDesignVersion().getPrice()).isEqualTo(expected.getDesignVersion().getPrice());
        assertThat(actual.getDesignVersion().getVersion()).isEqualTo(Version.DESIGN);
        assertThat(actual.getDesignVersion().getVersionNumber()).isEqualTo(1);
        assertThat(actual.getCreatedTime()).isNotNull();
        assertThat(actual.getUpdatedTime()).isNotNull();
    }

    @Test
    void deployProduct() {

        final ProductVersion productVersion = new ProductVersion("Gin Topic", "sku-001", "signature drink", BigDecimal.valueOf(350));
        final Product product = new Product(createdClient, productVersion);

        final Product createdProduct = productService.saveProduct(product);

        assertThat(createdProduct.getVersions()).hasSize(1);

        productService.deployProduct(createdProduct.getId());

        assertThat(createdProduct.getVersions()).hasSize(2);

        assertThat(createdProduct.getDesignVersion()).satisfies(version -> {
            assertThat(version).isNotNull();
            assertThat(version.getId()).contains("-2");
            assertThat(version.getVersionNumber()).isEqualTo(2);
            assertThat(version.getVersion()).isEqualTo(Version.DESIGN);
        });

        assertThat(createdProduct.getLiveVersion()).satisfies(version -> {
            assertThat(version).isNotNull();
            assertThat(version.getId()).contains("-1");
            assertThat(version.getVersionNumber()).isEqualTo(1);
            assertThat(version.getVersion()).isEqualTo(Version.LIVE);
        });

        assertThat(productVersionRepository.findAll()).hasSize(2);

        productService.deployProduct(createdProduct.getId());

        assertThat(productVersionRepository.findAll()).hasSize(2);

        assertThat(createdProduct.getLiveVersion().getVersionNumber()).isEqualTo(2);
        assertThat(createdProduct.getDesignVersion().getVersionNumber()).isEqualTo(3);
    }
}