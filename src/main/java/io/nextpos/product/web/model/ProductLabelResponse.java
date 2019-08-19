package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductLabel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductLabelResponse {

    private String id;

    private String value;

    private String label;

    private List<ProductLabelResponse> subLabels = new ArrayList<>();

    public ProductLabelResponse(final String id, final String label) {
        this.id = id;
        this.value = id;
        this.label = label;
    }

    public ProductLabelResponse addSubLabel(ProductLabel childProductLabel) {
        final ProductLabelResponse subLabel = new ProductLabelResponse(childProductLabel.getId(), childProductLabel.getName());
        subLabels.add(subLabel);

        return subLabel;
    }
}
