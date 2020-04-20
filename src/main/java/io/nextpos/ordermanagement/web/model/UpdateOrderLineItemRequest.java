package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.ProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.PositiveOrZero;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderLineItemRequest {

    @PositiveOrZero
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;

    public List<ProductSnapshot.ProductOptionSnapshot> toProductOptionSnapshots() {

        if (CollectionUtils.isEmpty(productOptions)) {
            return Collections.emptyList();
        }

        return productOptions.stream()
                .map(p -> new ProductSnapshot.ProductOptionSnapshot(p.getOptionName(), p.getOptionValue(), p.getOptionPrice()))
                .collect(Collectors.toList());
    }
}
