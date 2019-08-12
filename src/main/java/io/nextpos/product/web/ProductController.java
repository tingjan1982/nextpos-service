package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.*;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.ProductRequest;
import io.nextpos.product.web.model.ProductResponse;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final ProductLabelService productLabelService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public ProductController(final ProductService productService, final ProductLabelService productLabelService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.productService = productService;
        this.productLabelService = productLabelService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public ProductResponse createProduct(@RequestAttribute("req-client") Client client, @Valid @RequestBody ProductRequest productRequest) {

        final Product product = fromRequest(productRequest, client);
        final Product createdProduct = productService.saveProduct(product);

        return toResponse(createdProduct, Version.DESIGN);
    }

    private Product fromRequest(ProductRequest productRequest, Client client) {

        final ProductVersion productVersion = new ProductVersion(productRequest.getName(),
                productRequest.getSku(),
                productRequest.getDescription(),
                productRequest.getPrice());
        final Product product = new Product(client, productVersion);
        product.setProductLabel(resolveProductLabel(productRequest));

        return product;
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable String id,
                                      @RequestParam(value = "version", required = false, defaultValue = "DESIGN") Version version,
                                      @RequestAttribute("req-client") Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));

        return toResponse(product, version);
    }

    @PostMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable final String id,
                                         @RequestAttribute("req-client") Client client,
                                         @Valid @RequestBody ProductRequest productRequest) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        updateProductFromRequest(product, productRequest);

        productService.saveProduct(product);

        return toResponse(product, Version.DESIGN);
    }

    private void updateProductFromRequest(final Product product, final ProductRequest productRequest) {
        final ProductVersion designVersion = product.getDesignVersion();
        designVersion.setProductName(productRequest.getName());
        designVersion.setSku(productRequest.getSku());
        designVersion.setDescription(productRequest.getDescription());
        designVersion.setPrice(productRequest.getPrice());

        product.setProductLabel(resolveProductLabel(productRequest));
    }


    @PostMapping("/{id}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deployProduct(@PathVariable String id) {

        productService.deployProduct(id);
    }

    private ProductLabel resolveProductLabel(ProductRequest productRequest) {
        if (productRequest.getProductLabelId() != null) {
            return productLabelService.getProductLabel(productRequest.getProductLabelId()).orElseThrow(() -> {
                throw new ObjectNotFoundException(productRequest.getProductLabelId(), ProductLabel.class);
            });
        }

        return null;
    }

    private ProductResponse toResponse(Product product, final Version version) {

        ProductVersion productVersion = product.getObjectByVersion(version).orElseThrow(() -> {
            throw new ObjectNotFoundException(product.getId() + "-" +  version, ProductOptionVersion.class);
        });

        final ProductLabel productLabel = product.getProductLabel();

        return new ProductResponse(product.getId(),
                productVersion.getId(),
                productVersion.getProductName(),
                version,
                productVersion.getSku(),
                productVersion.getDescription(),
                productVersion.getPrice(),
                productLabel != null ? productLabel.getName() : null);
    }
}
