package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.product.data.*;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Transactional
class ProductOptionServiceImplTest {

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductOptionVersionRepository productOptionVersionRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientRepository.save(client);
    }

    @Test
    void createAndGetProductOption_OneChoice() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("ice level", ProductOptionVersion.OptionType.ONE_CHOICE, true);
        stagingProductOption.addOptionValue("full");
        stagingProductOption.addOptionValue("3/4");
        stagingProductOption.addOptionValue("half");
        stagingProductOption.addOptionValue("1/3");
        stagingProductOption.addOptionValue("none");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        verifyProductOption(productOptionService.getProductOption(createdProductOption.getId()));
    }

    private void verifyProductOption(final ProductOption productOption) {
        assertThat(productOption.getId()).isNotNull();
        assertThat(productOption.getClient()).satisfies(c -> {
            assertThat(c).isNotNull();
            assertThat(c.getId()).isNotNull();
        });

        assertThat(productOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.ONE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(5);
        });
    }

    @Test
    void createAndGetProductOption_MultipleChoice() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.MULTIPLE_CHOICE, true);
        stagingProductOption.addOptionValue("cheese");
        stagingProductOption.addOptionValue("egg");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.MULTIPLE_CHOICE);
            assertThat(staging.getOptionValues()).hasSize(2);
        });
    }

    @Test
    void createAndGetProductOption_FreeText() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT, true);
        stagingProductOption.addOptionValue("this shouldn't have any effect", BigDecimal.valueOf(50));

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).satisfies(staging -> {
            assertThat(staging.getId()).contains(productOption.getId());
            assertThat(staging.getOptionType()).isEqualTo(ProductOptionVersion.OptionType.FREE_TEXT);
            assertThat(staging.getOptionValues()).isEmpty();
        });
    }

    @Test
    public void deployProductOption() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.MULTIPLE_CHOICE, true);
        stagingProductOption.addOptionValue("cheese");
        stagingProductOption.addOptionValue("egg");

        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        final ProductOption createdProductOption = productOptionService.createProductOption(productOption);

        assertThat(createdProductOption.getDesignVersion()).isNotNull();
        assertThrows(ObjectNotFoundException.class, createdProductOption::getLiveVersion);

        productOptionService.getProductOption(createdProductOption.getId());

        final ProductOption updated = productOptionService.deployProductOption(createdProductOption.getId());

        assertThat(updated.getLiveVersion()).satisfies(po -> {
            assertThat(po).isNotNull();
            assertThat(po.getId()).contains("-1"); // check that the version is incremented.
            assertThat(po.getVersion()).isEqualTo(Version.LIVE);
        });

        assertThat(updated.getDesignVersion()).satisfies(po -> {
            assertThat(po).isNotNull();
            assertThat(po.getId()).contains("-2"); // check that the version is incremented.
            assertThat(po.getVersion()).isEqualTo(Version.DESIGN);
        });

        productOptionService.deployProductOption(createdProductOption.getId());

        assertThat(productOptionVersionRepository.findAll()).hasSize(2);
    }

    @Test
    public void addProductOptionToProduct() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT, true);
        final ProductOption productOption = new ProductOption(client, stagingProductOption);

        productOptionService.createProductOption(productOption);

        final Product product = new Product(client, DummyObjects.dummyProductVersion());

        productService.saveProduct(product);

        final List<ProductOptionRelation> productOptionRelations = productOptionService.addProductOptionToProduct(productOption, Collections.singletonList(product));

        assertThat(productOptionRelations).satisfies(por -> {
            assertThat(por).isInstanceOf(ProductOptionRelation.ProductOptionOfProduct.class);
            assertThat(por.getId()).isNotNull();
            assertThat(por.getProductOption()).isEqualTo(productOption);
            assertThat(((ProductOptionRelation.ProductOptionOfProduct) por).getProduct()).isEqualTo(product);

        }, Index.atIndex(0));

        final Product retrievedProduct = productService.getProduct(product.getId());
        assertThat(retrievedProduct.getProductOptionOfProducts()).hasSize(1);
    }

    @Test
    public void addProductOptionToProductLabel() {

        final ProductOptionVersion stagingProductOption = new ProductOptionVersion("extras", ProductOptionVersion.OptionType.FREE_TEXT, true);
        final ProductOption productOption = new ProductOption(client, stagingProductOption);
        productOptionService.createProductOption(productOption);

        final ProductLabel drinks = new ProductLabel("drinks", client);
        productLabelService.createProductLabel(drinks);

        final List<ProductOptionRelation> productOptionRelations = productOptionService.addProductOptionToProductLabel(productOption, Collections.singletonList(drinks));

        assertThat(productOptionRelations).satisfies(por -> {
            assertThat(por).isInstanceOf(ProductOptionRelation.ProductOptionOfLabel.class);
            assertThat(por.getId()).isNotNull();
            assertThat(por.getProductOption()).isEqualTo(productOption);
            assertThat(((ProductOptionRelation.ProductOptionOfLabel) por).getProductLabel()).isEqualTo(drinks);

        }, Index.atIndex(0));

        final ProductLabel productLabel = productLabelService.getProductLabelOrThrows(drinks.getId());
        assertThat(productLabel.getProductOptionOfLabels()).hasSize(1);
    }
}