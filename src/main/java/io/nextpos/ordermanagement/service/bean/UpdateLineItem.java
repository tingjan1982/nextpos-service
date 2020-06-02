package io.nextpos.ordermanagement.service.bean;

import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class UpdateLineItem {

    private String lineItemId;

    private int quantity;

    private BigDecimal overridePrice;

    private List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots;

    private ProductLevelOffer.GlobalProductDiscount globalProductDiscount;

    private BigDecimal discountValue;
}
