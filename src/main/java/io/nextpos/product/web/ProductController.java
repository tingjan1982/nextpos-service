package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.inventorymanagement.web.model.InventoryResponse;
import io.nextpos.product.data.*;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductSearchService;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.*;
import io.nextpos.product.web.util.ObjectWithProductOptionVisitorWrapper;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final InventoryService inventoryService;

    private final ProductSearchService productSearchService;

    private final ProductLabelService productLabelService;

    private final WorkingAreaService workingAreaService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper;

    @Autowired
    public ProductController(final ProductService productService, InventoryService inventoryService, final ProductSearchService productSearchService, final ProductLabelService productLabelService, final WorkingAreaService workingAreaService, final ClientObjectOwnershipService clientObjectOwnershipService, final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.productSearchService = productSearchService;
        this.productLabelService = productLabelService;
        this.workingAreaService = workingAreaService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.productOptionVisitorWrapper = productOptionVisitorWrapper;
    }

    @PostMapping
    public ProductResponse createProduct(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @Valid @RequestBody ProductRequest productRequest) {

        final Product product = fromRequest(productRequest, client);
        final Product createdProduct = productService.saveProduct(product);

        String sku = productRequest.getSku();

        if (StringUtils.isNotBlank(sku)) {
            final CreateInventory createInventory = new CreateInventory(client.getId(), createdProduct.getId(), List.of(Inventory.InventoryQuantity.each(sku, BigDecimal.ZERO)));
            inventoryService.createStock(createInventory);
        }

        return toResponse(createdProduct, Version.DESIGN);
    }

    private Product fromRequest(ProductRequest productRequest, Client client) {

        Product.ProductBuilder<?, ?> builder;

        if (!CollectionUtils.isEmpty(productRequest.getProductComboLabels())) {
            builder = ProductCombo.builder(client);

        } else if (!CollectionUtils.isEmpty(productRequest.getChildProducts())) {
            final ProductSet.ProductSetBuilder productSetBuilder = ProductSet.builder(client);
            productRequest.getChildProducts().forEach(pid -> productSetBuilder.addChildProduct(productService.getProduct(pid)));
            builder = productSetBuilder;

        } else {
            builder = Product.builder(client);
        }

        final Product product = builder.productNameAndPrice(productRequest.getName(), productRequest.getPrice())
                .internalProductName(productRequest.getInternalName())
                .sku(productRequest.getSku())
                .description(productRequest.getDescription())
                .costPrice(productRequest.getCostPrice()).build();

        final ProductLabel resolvedLabel = resolveProductLabel(client, productRequest.getProductLabelId());
        product.setProductLabel(resolvedLabel);

        final WorkingArea resolvedWorkingArea = resolveWorkingArea(client, productRequest.getWorkingAreaId());
        product.setWorkingArea(resolvedWorkingArea);

        productOptionVisitorWrapper.accept(product, productRequest.getProductOptionIds());

        if (product instanceof ProductCombo) {
            productService.saveProduct(product);
            
            final ProductCombo productCombo = (ProductCombo) product;

            productRequest.getProductComboLabels().forEach(cl -> {
                final ProductLabel productLabel = productLabelService.getProductLabelOrThrows(cl.getProductLabelId());

                final ProductCombo.ProductComboLabel comboLabel = productCombo.addProductComboLabel(productLabel);
                comboLabel.setMultipleSelection(cl.isMultipleSelection());
                comboLabel.setOrdering(cl.getOrdering());
            });
        }

        return product;
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable String id,
                                      @RequestParam(value = "version", required = false, defaultValue = "DESIGN") Version version,
                                      @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));

        return toResponse(product, version);
    }

    @GetMapping
    public ProductsResponse searchProducts(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @RequestParam("keyword") String keyword) {

        final List<LightProductResponse> results = productSearchService.getProductsByKeyword(client, Version.DESIGN, keyword).stream()
                .map(LightProductResponse::new)
                .collect(Collectors.toList());

        return new ProductsResponse(results);
    }

    @PostMapping("/{id}")
    public ProductResponse updateProduct(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @PathVariable final String id,
                                         @Valid @RequestBody ProductRequest productRequest) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        updateProductFromRequest(client, product, productRequest);

        productService.saveProduct(product);

        return toResponse(product, Version.DESIGN);
    }

    @PostMapping("/{id}/label")
    public ProductResponse changeProductLabel(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @PathVariable final String id,
                                              @Valid @RequestBody ChangeLabelRequest productRequest) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        final ProductLabel resolvedLabel = resolveProductLabel(client, productRequest.getProductLabelId());
        product.setProductLabel(resolvedLabel);

        productService.saveProduct(product);

        return toResponse(product, Version.DESIGN);
    }

    private void updateProductFromRequest(final Client client, final Product product, final ProductRequest productRequest) {

        final ProductVersion designVersion = product.getDesignVersion();
        designVersion.setProductName(productRequest.getName());
        designVersion.setInternalProductName(productRequest.getInternalName());
        designVersion.setSku(productRequest.getSku());
        designVersion.setDescription(productRequest.getDescription());
        designVersion.setPrice(productRequest.getPrice());
        designVersion.setCostPrice(productRequest.getCostPrice());

        final ProductLabel resolvedLabel = resolveProductLabel(client, productRequest.getProductLabelId());
        product.setProductLabel(resolvedLabel);

        final WorkingArea resolvedWorkingArea = resolveWorkingArea(client, productRequest.getWorkingAreaId());
        product.setWorkingArea(resolvedWorkingArea);

        productOptionVisitorWrapper.accept(product, productRequest.getProductOptionIds());

        if (product instanceof ProductSet) {
            final ProductSet productSet = (ProductSet) product;
            productSet.clearChildProducts();

            if (productRequest.getChildProducts() != null) {
                productRequest.getChildProducts().forEach(pid -> productSet.addChildProduct(productService.getProduct(pid)));
            }
        }

        if (product instanceof ProductCombo) {
            final ProductCombo productCombo = (ProductCombo) product;
            productCombo.clearProductComboLabels();

            if (productRequest.getProductComboLabels() != null) {
                productRequest.getProductComboLabels().forEach(cl -> {
                    final ProductLabel productLabel = productLabelService.getProductLabelOrThrows(cl.getProductLabelId());

                    final ProductCombo.ProductComboLabel comboLabel = productCombo.addProductComboLabel(productLabel);
                    comboLabel.setMultipleSelection(cl.isMultipleSelection());
                    comboLabel.setOrdering(cl.getOrdering());
                });
            }
        }
    }

    private ProductLabel resolveProductLabel(final Client client, String labelId) {
        if (labelId != null) {
            final ProductLabel productLabel = clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(labelId));

            if (!productLabel.getChildLabels().isEmpty()) {
                throw new BusinessLogicException("message.hasChildLabel", "Product label with child label cannot be assigned to product directly.");
            }

            return productLabel;
        }

        return null;
    }

    private WorkingArea resolveWorkingArea(Client client, String workingAreaId) {
        if (workingAreaId != null) {
            return clientObjectOwnershipService.checkOwnership(client, () -> workingAreaService.getWorkingArea(workingAreaId));
        }

        return null;
    }

    @GetMapping("/{id}/image")
    public void getProductImage(@PathVariable final String id,
                                @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                HttpServletResponse response) throws IOException {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        final ProductVersion designVersion = product.getDesignVersion();

        if (designVersion.getProductImage() != null) {
            FileCopyUtils.copy(designVersion.getProductImage().getBinaryData(), response.getOutputStream());
        } else {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    @PostMapping("/{id}/image")
    public ProductResponse uploadProductImage(@PathVariable final String id,
                                              @RequestParam("image") MultipartFile imageFile,
                                              @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) throws Exception {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        final ProductVersion designVersion = product.getDesignVersion();
        designVersion.updateProductImage(imageFile.getBytes());

        final Product updatedProduct = productService.saveProduct(product);

        return toResponse(updatedProduct, Version.DESIGN);
    }

    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductImage(@PathVariable final String id,
                                   @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        final ProductVersion designVersion = product.getDesignVersion();
        designVersion.deleteProductImage();

        productService.saveProduct(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable final String id,
                              @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));

        productService.deleteProduct(product);
    }

    @PostMapping("/{id}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deployProduct(@PathVariable String id) {

        productService.deployProduct(id);
    }

    @PostMapping("/{id}/togglePin")
    public ProductResponse togglePin(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @PathVariable final String id) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        product.setPinned(!product.isPinned());

        return toResponse(productService.saveProduct(product), Version.DESIGN);
    }

    @PostMapping("/{id}/toggleOutOfStock")
    public ProductResponse toggleOutOfStock(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        product.setOutOfStock(!product.isOutOfStock());

        return toResponse(productService.saveProduct(product), Version.DESIGN);
    }

    @PostMapping("/ordering")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderProducts(@RequestBody OrderProductsRequest request) {

        productService.reorderProducts(request.getProductIds());
    }


    private ProductResponse toResponse(Product product, final Version version) {

        ProductVersion productVersion = product.getObjectByVersionThrows(version);
        final ProductResponse productResponse = new ProductResponse(product, productVersion);

        if (product instanceof ProductCombo) {
            final ProductCombo productCombo = (ProductCombo) product;
            final List<ProductComboLabelResponse> combos = productCombo.getProductComboLabels().stream()
                    .map(ProductComboLabelResponse::new)
                    .collect(Collectors.toList());

            productResponse.setProductComboLabels(combos);
        }

        if (product instanceof ProductSet) {
            productResponse.setChildProducts(ChildProduct.toChildProducts(((ProductSet) product)));
        }

        inventoryService.getInventoryByProductId(product.getClient().getId(), product.getId()).ifPresent(inv -> {
            productResponse.setInventory(new InventoryResponse(inv));
        });

        return productResponse;
    }
}
