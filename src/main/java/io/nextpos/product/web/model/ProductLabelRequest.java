package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductLabelRequest {

    @NotBlank
    private String label;

    private List<String> productOptionIds;

    private List<ProductLabelRequest> subLabels;
}
