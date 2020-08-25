package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.*;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductSearchService;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.*;
import io.nextpos.product.web.util.ObjectWithProductOptionVisitorWrapper;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final ProductSearchService productSearchService;

    private final ProductLabelService productLabelService;

    private final WorkingAreaService workingAreaService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper;

    @Autowired
    public ProductController(final ProductService productService, final ProductSearchService productSearchService, final ProductLabelService productLabelService, final WorkingAreaService workingAreaService, final ClientObjectOwnershipService clientObjectOwnershipService, final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper) {
        this.productService = productService;
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

        return toResponse(createdProduct, Version.DESIGN);
    }

    private Product fromRequest(ProductRequest productRequest, Client client) {

        Product.ProductBuilder<?, ?> builder;

        if (!CollectionUtils.isEmpty(productRequest.getChildProducts())) {
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
                .map(p -> {
                    ProductLabel productLabel = p.getProduct().getProductLabel();
                    String productLabelId = productLabel != null ? productLabel.getId() : null;

                    return new LightProductResponse(p.getProduct().getId(),
                            ProductType.resolveProductType(p.getProduct()),
                            p.getProductName(),
                            p.getPrice(),
                            productLabelId,
                            p.getProduct().isPinned());
                }).collect(Collectors.toList());

        return new ProductsResponse(results);
    }

    @PostMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable final String id,
                                         @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @Valid @RequestBody ProductRequest productRequest) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        updateProductFromRequest(client, product, productRequest);

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
    }

    private ProductLabel resolveProductLabel(final Client client, String labelId) {
        if (labelId != null) {
            return clientObjectOwnershipService.checkOwnership(client, () -> productLabelService.getProductLabelOrThrows(labelId));
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
    public ProductResponse pinProduct(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                      @PathVariable final String id) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        product.setPinned(!product.isPinned());

        return toResponse(productService.saveProduct(product), Version.DESIGN);
    }


    private ProductResponse toResponse(Product product, final Version version) {

        ProductVersion productVersion = product.getObjectByVersionThrows(version);

        final ProductLabel productLabel = product.getProductLabel();
        final WorkingArea workingArea = product.getWorkingArea();

        final List<String> productOptionIds = product.getProductOptionOfProducts().stream()
                .map(po -> po.getProductOption().getId()).collect(Collectors.toList());

        final List<ProductOptionResponse> productOptions = product.getProductOptionOfProducts().stream()
                .map(po -> toProductOptionResponse(version, po))
                .collect(Collectors.toList());

        final ProductResponse productResponse = new ProductResponse(product.getId(),
                ProductType.resolveProductType(product),
                productVersion.getId(),
                productVersion.getProductName(),
                productVersion.getInternalProductName(),
                version,
                productVersion.getSku(),
                productVersion.getDescription(),
                productVersion.getPrice(),
                productVersion.getCostPrice(),
                productLabel != null ? productLabel.getId() : null,
                productLabel != null ? productLabel.getName() : null,
                workingArea != null ? workingArea.getId() : null,
                productOptionIds,
                productOptions,
                product.isPinned());

        if (product instanceof ProductSet) {
            final List<ProductResponse.ChildProduct> childProducts = ((ProductSet) product).getChildProducts().stream()
                    .map(cp -> new ProductResponse.ChildProduct(cp.getId(),
                            cp.getDesignVersion().getProductName(),
                            cp.getDesignVersion().getInternalProductName())).collect(Collectors.toList());

            productResponse.setChildProducts(childProducts);
        }

        return productResponse;
    }

    private ProductOptionResponse toProductOptionResponse(final Version version, final ProductOptionRelation.ProductOptionOfProduct po) {

        final ProductOptionVersion productOptionVersion = po.getProductOption().getObjectByVersionThrows(version);
        return ProductOptionResponse.fromProductOptionVersion(productOptionVersion);
    }
}
