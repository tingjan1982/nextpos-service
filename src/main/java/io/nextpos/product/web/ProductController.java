package io.nextpos.product.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.product.data.*;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductOptionService;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.ProductOptionResponse;
import io.nextpos.product.web.model.ProductOptionValueModel;
import io.nextpos.product.web.model.ProductRequest;
import io.nextpos.product.web.model.ProductResponse;
import io.nextpos.product.web.util.ObjectWithProductOptionVisitorWrapper;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    private final ProductLabelService productLabelService;

    private final ProductOptionService productOptionService;

    private final WorkingAreaService workingAreaService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper;

    @Autowired
    public ProductController(final ProductService productService, final ProductLabelService productLabelService, final ProductOptionService productOptionService, final WorkingAreaService workingAreaService, final ClientObjectOwnershipService clientObjectOwnershipService, final ObjectWithProductOptionVisitorWrapper productOptionVisitorWrapper) {
        this.productService = productService;
        this.productLabelService = productLabelService;
        this.productOptionService = productOptionService;
        this.workingAreaService = workingAreaService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.productOptionVisitorWrapper = productOptionVisitorWrapper;
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
                                      @RequestAttribute("req-client") Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));

        return toResponse(product, version);
    }

    @PostMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable final String id,
                                         @RequestAttribute("req-client") Client client,
                                         @Valid @RequestBody ProductRequest productRequest) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));
        updateProductFromRequest(client, product, productRequest);

        productService.saveProduct(product);

        return toResponse(product, Version.DESIGN);
    }

    private void updateProductFromRequest(final Client client, final Product product, final ProductRequest productRequest) {

        final ProductVersion designVersion = product.getDesignVersion();
        designVersion.setProductName(productRequest.getName());
        designVersion.setSku(productRequest.getSku());
        designVersion.setDescription(productRequest.getDescription());
        designVersion.setPrice(productRequest.getPrice());

        final ProductLabel resolvedLabel = resolveProductLabel(client, productRequest.getProductLabelId());
        product.setProductLabel(resolvedLabel);

        final WorkingArea resolvedWorkingArea = resolveWorkingArea(client, productRequest.getWorkingAreaId());
        product.setWorkingArea(resolvedWorkingArea);

        productOptionVisitorWrapper.accept(product, productRequest.getProductOptionIds());
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable final String id,
                              @RequestAttribute("req-client") Client client) {

        final Product product = clientObjectOwnershipService.checkOwnership(client, () -> productService.getProduct(id));

        productService.deleteProduct(product);
    }


    @PostMapping("/{id}/deploy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deployProduct(@PathVariable String id) {

        productService.deployProduct(id);
    }

    private ProductResponse toResponse(Product product, final Version version) {

        ProductVersion productVersion = product.getObjectByVersionThrows(version);

        final ProductLabel productLabel = product.getProductLabel();
        final WorkingArea workingArea = product.getWorkingArea();

        final List<ProductOptionResponse> productOptions = product.getProductOptionOfProducts().stream()
                .filter(pop -> pop.getProduct() != null)
                .map(po -> toProductOptionResponse(version, po))
                .collect(Collectors.toList());

        return new ProductResponse(product.getId(),
                productVersion.getId(),
                productVersion.getProductName(),
                version,
                productVersion.getSku(),
                productVersion.getDescription(),
                productVersion.getPrice(),
                productLabel != null? productLabel.getId() : null,
                productLabel != null ? productLabel.getName() : null,
                workingArea != null ? workingArea.getName() : null,
                productOptions);
    }

    private ProductOptionResponse toProductOptionResponse(final Version version, final ProductOptionRelation.ProductOptionOfProduct po) {

        final ProductOptionVersion productOptionVersion = po.getProductOption().getObjectByVersionThrows(version);
        final List<ProductOptionValueModel> optionValues = productOptionVersion.getOptionValues().stream()
                .map(pov -> new ProductOptionValueModel(pov.getOptionValue(), pov.getOptionPrice()))
                .collect(Collectors.toList());

        return new ProductOptionResponse(po.getProductOption().getId(),
                productOptionVersion.getId(),
                productOptionVersion.getOptionName(),
                version,
                productOptionVersion.getOptionType(),
                optionValues);
    }
}
