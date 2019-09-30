package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductLabel;
import io.nextpos.shared.web.model.SimpleObjectResponse;
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

    private SimpleObjectResponse workingArea;

    private List<SimpleObjectResponse> productOptions;

    private List<ProductLabelResponse> subLabels = new ArrayList<>();

    private List<SimpleObjectResponse> appliedProducts = new ArrayList<>();


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
