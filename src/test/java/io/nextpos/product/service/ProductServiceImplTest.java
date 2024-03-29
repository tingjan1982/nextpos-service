package io.nextpos.product.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.product.data.*;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductServiceImplTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductVersionRepository productVersionRepository;

    @Autowired
    private ProductComboRepository productComboRepository;

    @Autowired
    private ClientService clientService;

    private Client createdClient;

    @BeforeEach
    public void prepare() {
        final Client client = DummyObjects.dummyClient();
        createdClient = clientService.saveClient(client);
    }

    @Test
    void crudProduct() {

        final ProductOption ice = new ProductOption(createdClient, new ProductOptionVersion("ice", ProductOptionVersion.OptionType.ONE_CHOICE, true));
        productOptionService.saveProductOption(ice);

        final ProductOption sugar = new ProductOption(createdClient, new ProductOptionVersion("ice", ProductOptionVersion.OptionType.ONE_CHOICE, true));
        productOptionService.saveProductOption(sugar);

        final ProductLabel label = new ProductLabel("label", createdClient);
        label.replaceProductOptions(ice, sugar);

        productLabelService.saveProductLabel(label);

        assertThat(label.getProductOptionOfLabels()).hasSize(2);

        final ProductVersion productVersion = new ProductVersion("Gin & Tonic", "sku-001", "signature drink", BigDecimal.valueOf(350));
        final Product product = new Product(createdClient, productVersion);
        product.setProductLabel(label);

        final Product createdProduct = productService.saveProduct(product);

        assertProduct(createdProduct, product);

        final Product existingProduct = productService.getProduct(createdProduct.getId());
        assertProduct(existingProduct, product);
        assertThat(existingProduct.getProductOptionOfProducts()).hasSize(2);

        existingProduct.setProductLabel(null);
        existingProduct.getDesignVersion().setProductName("updated");

        productService.saveProduct(existingProduct);

        final Product updatedProduct = productService.getProduct(existingProduct.getId());

        assertThat(updatedProduct.getId()).isEqualTo(createdProduct.getId());
        assertThat(updatedProduct.getProductLabel()).isNull();
        assertThat(updatedProduct.getProductOptionOfProducts()).hasSize(2);
        assertThat(updatedProduct.getDesignVersion().getProductName()).isEqualTo("updated");

        productService.deleteProduct(updatedProduct);

        assertThatThrownBy(() -> productService.getProduct(updatedProduct.getId())).isInstanceOf(ObjectNotFoundException.class);
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
    void orderProducts() {

        final ProductLabel label = new ProductLabel("label", createdClient);
        productLabelService.saveProductLabel(label);

        final Product tea = Product.builder(createdClient).productNameAndPrice("tea", new BigDecimal("50")).build();
        productService.saveProduct(tea);
        final Product coffee = Product.builder(createdClient).productNameAndPrice("coffee", new BigDecimal("50")).build();
        productService.saveProduct(coffee);

        productService.reorderProducts(List.of(coffee.getId(), tea.getId()));

        assertThat(productService.getProduct(coffee.getId()).getOrdering()).isEqualTo(1);
        assertThat(productService.getProduct(tea.getId()).getOrdering()).isEqualTo(2);
    }

    @Test
    void createProductSet() {

        final Product coffee = Product.builder(createdClient).productNameAndPrice("Coffee", BigDecimal.valueOf(50)).build();
        productService.saveProduct(coffee);

        final Product bagel = Product.builder(createdClient).productNameAndPrice("Bagel", BigDecimal.valueOf(70)).build();
        productService.saveProduct(bagel);

        final ProductSet cmb = ProductSet.builder(createdClient).productNameAndPrice("Coffee meets Bagel", BigDecimal.valueOf(100)).build();
        cmb.addChildProduct(coffee);
        cmb.addChildProduct(bagel);
        productService.saveProductSet(cmb);

        assertThat(productService.getProduct(cmb.getId())).satisfies(p -> {
            assertThat(p).isNotNull();
            assertThat(p).isInstanceOf(ProductSet.class);
        });

        assertThat(productService.getProductSet(cmb.getId())).satisfies(p -> {
            assertThat(p.getChildProducts()).hasSize(2);
        });

        cmb.removeChildProduct(bagel);
        productService.saveProductSet(cmb);

        assertThat(productService.getProductSet(cmb.getId())).satisfies(p -> assertThat(p.getChildProducts()).hasSize(1));
    }

    @Test
    void createProductCombo() {

        final ProductLabel foodLabel = new ProductLabel("Food", createdClient);
        productLabelService.saveProductLabel(foodLabel);

        final ProductLabel drinkLabel = new ProductLabel("Drinks", createdClient);
        productLabelService.saveProductLabel(drinkLabel);

        final Product coffee = Product.builder(createdClient).productNameAndPrice("Coffee", BigDecimal.valueOf(50))
                .productLabel(drinkLabel).build();
        productService.saveProduct(coffee);

        assertThat(coffee.getProductLabel()).isEqualTo(drinkLabel);

        final ProductCombo combo = ProductCombo.builder(createdClient).productNameAndPrice("Combo", BigDecimal.ZERO).build();

        productService.saveProductCombo(combo);

        assertThat(combo.getId()).isNotNull();

        combo.addProductComboLabel(foodLabel).setOrdering(2);
        combo.addProductComboLabel(drinkLabel).setOrdering(1);

        productService.saveProductCombo(combo);

        assertThat(productService.getProductCombo(combo.getId())).satisfies(pc -> {
            assertThat(pc.getProductComboLabels()).hasSize(2);
            assertThat(pc.getProductComboLabels()).isSortedAccordingTo(Comparator.comparing(ProductCombo.ProductComboLabel::getOrdering));
            final ProductCombo.ProductComboLabel comboLabelToCompare = pc.getProductComboLabel(drinkLabel).orElseThrow();
            assertThat(comboLabelToCompare.getProductCombo()).isEqualTo(combo);
            assertThat(comboLabelToCompare.getProductLabel()).isEqualTo(drinkLabel);
            assertThat(comboLabelToCompare.isMultipleSelection()).isFalse();

        });

        combo.clearProductComboLabels();

        productService.saveProductCombo(combo);

        assertThat(productService.getProductCombo(combo.getId()).getProductComboLabels()).isEmpty();

        productService.deleteProduct(combo);

        assertThat(productComboRepository.findAll()).isEmpty();

        assertThat(productLabelService.getProductLabel(foodLabel.getId())).isNotNull();
        assertThat(productLabelService.getProductLabel(drinkLabel.getId())).isNotNull();
    }

    @Test
    void saveProductVariations() {

        final VariationDefinition size = new VariationDefinition(createdClient, "size");
        size.addAttributes(Arrays.asList("small", "medium", "large"));
        final VariationDefinition savedVariationDefinition = productService.saveVariationDefinition(size);

        assertThat(savedVariationDefinition).satisfies(v -> {
            assertThat(v.getId()).isNotNull();
            assertThat(v.getAttributes()).hasSize(3);
        });

        final ParentProduct parentProduct = ParentProduct.builder(createdClient, size)
                .productNameAndPrice("fried noodle", new BigDecimal(100))
                .addVariation("small", new BigDecimal(80))
                .addVariation("large", new BigDecimal(120))
                .build();

        productService.saveParentProduct(parentProduct);

        assertThat(parentProduct).satisfies(p -> {
            assertThat(p.getId()).isNotNull();
            assertThat(p.getProductVariations()).hasSize(3);
            assertThat(p.getProductVariation("small")).isNotEmpty();
            assertThat(p.getProductVariation("medium")).isNotEmpty();
            assertThat(p.getProductVariation("large")).isNotEmpty();
        });

        assertThat(parentProduct.getProductVariation("small").orElseThrow().getDesignVersion().getPrice()).isEqualByComparingTo("80");
        assertThat(parentProduct.getProductVariation("medium").orElseThrow().getDesignVersion().getPrice()).isEqualByComparingTo("100");
        assertThat(parentProduct.getProductVariation("large").orElseThrow().getDesignVersion().getPrice()).isEqualByComparingTo("120");
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