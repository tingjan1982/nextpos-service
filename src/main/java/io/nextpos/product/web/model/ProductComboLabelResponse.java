package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductCombo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductComboLabelResponse {

    private String labelId;

    private String labelName;

    private boolean multipleSelection;

    private int ordering;

    public ProductComboLabelResponse(ProductCombo.ProductComboLabel comboLabel) {

        this.labelId = comboLabel.getProductLabel().getId();
        this.labelName = comboLabel.getProductLabel().getName();
        this.multipleSelection = comboLabel.isMultipleSelection();
        this.ordering = comboLabel.getOrdering();
    }
}
