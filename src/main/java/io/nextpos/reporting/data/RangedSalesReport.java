package io.nextpos.reporting.data;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class RangedSalesReport {

    private RangeType rangeType;

    private List<TotalSales> totalSales = new ArrayList<>();

    private List<SalesByRange> salesByRange = new ArrayList<>();

    private List<SalesByProduct> salesByProduct;

    public boolean hasResult() {
        return !CollectionUtils.isEmpty(totalSales);
    }

    public TotalSales getTotalSales() {
        return hasResult() ? totalSales.get(0) : new TotalSales();
    }

    @Data
    public static class TotalSales {

        private String id;

        private BigDecimal salesTotal = BigDecimal.ZERO;
    }

    @Data
    public static class SalesByRange {

        private String id;

        private LocalDate date;

        private BigDecimal total = BigDecimal.ZERO;

        public static SalesByRange emptyObject(final String id, LocalDate date) {
            final SalesByRange salesByRange = new SalesByRange();
            salesByRange.setId(id);
            salesByRange.setDate(date);

            return salesByRange;
        }
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
        WEEK, MONTH
    }
}
