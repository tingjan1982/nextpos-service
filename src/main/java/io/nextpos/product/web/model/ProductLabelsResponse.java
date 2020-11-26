package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductLabel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductLabelsResponse {

    private List<LightProductLabelResponse> labels;

    @Data
    public static class LightProductLabelResponse {

        private final String id;

        private final String value;

        private final String label;

        private List<LightProductLabelResponse> subLabels = new ArrayList<>();

        public LightProductLabelResponse(String id, String label) {
            this.id = id;
            this.value = id;
            this.label = label;
        }

        public LightProductLabelResponse addSubLabel(ProductLabel childProductLabel) {
            final LightProductLabelResponse subLabel = new LightProductLabelResponse(childProductLabel.getId(), childProductLabel.getName());
            subLabels.add(subLabel);

            return subLabel;
        }
    }
}
