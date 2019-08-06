package io.nextpos.ordertransaction.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillLineItemBean {

    private String name;

    private int quantity;

    private BigDecimal subTotal;
}
