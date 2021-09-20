package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductCombo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductComboLabelResponse {

    private String id;

    private String name;

    private boolean multipleSelection;

    private int ordering;

    public ProductComboLabelResponse(ProductCombo.ProductComboLabel comboLabel) {

        this.id = comboLabel.getProductLabel().getId();
        this.name = comboLabel.getProductLabel().getName();
        this.multipleSelection = comboLabel.isMultipleSelection();
        this.ordering = comboLabel.getOrdering();
    }
}
