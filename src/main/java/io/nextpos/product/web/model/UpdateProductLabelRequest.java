package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductLabelRequest {

    @NotBlank
    private String label;

    private String workingAreaId;

    private List<String> productOptionIds;

    private boolean appliesToProducts;
}
