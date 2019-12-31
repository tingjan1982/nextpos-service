package io.nextpos.reporting.data;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RangedSalesReport {

    private RangeType rangeType;

    private List<TotalSales> totalSales;

    private List<SalesByRange> salesByRange;

    private List<SalesByProduct> salesByProduct;

    public boolean hasResult() {
        return !CollectionUtils.isEmpty(totalSales);
    }

    public TotalSales getTotalSales() {
        return totalSales.get(0);
    }

    @Data
    public static class TotalSales {

        private String id;

        private BigDecimal salesTotal;
    }

    @Data
    public static class SalesByRange {

        private String id;

        private String label;

        private LocalDate date;

        private String formattedDate;

        private BigDecimal total;
    }

    @Data
    public static class SalesByProduct {

        private String id;

        private String productName;

        private int salesQuantity;

        private BigDecimal productSales;

        private BigDecimal percentage;
    }


    public enum RangeType {
        WEEK
    }
}
