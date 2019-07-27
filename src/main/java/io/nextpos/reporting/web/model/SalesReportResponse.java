package io.nextpos.reporting.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {

    private String id;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private BigDecimal salesTotal;

    /**
     * key is product name
     * value is sales total of the product
     */
    private Map<String, BigDecimal> salesByProducts;
}
