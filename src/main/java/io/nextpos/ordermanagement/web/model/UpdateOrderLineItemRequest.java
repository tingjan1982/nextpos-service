package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class UpdateOrderLineItemRequest {

    @PositiveOrZero
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;

    private ProductLevelOffer.GlobalProductDiscount productDiscount;

    private BigDecimal discountValue;

    public List<ProductSnapshot.ProductOptionSnapshot> toProductOptionSnapshots() {

        if (CollectionUtils.isEmpty(productOptions)) {
            return Collections.emptyList();
        }

        return productOptions.stream()
                .map(p -> new ProductSnapshot.ProductOptionSnapshot(p.getOptionName(), p.getOptionValue(), p.getOptionPrice()))
                .collect(Collectors.toList());
    }
}
