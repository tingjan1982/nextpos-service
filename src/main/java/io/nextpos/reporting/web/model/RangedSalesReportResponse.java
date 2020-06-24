package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.ZonedDateRange;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class RangedSalesReportResponse {

    private ZonedDateRange dateRange;

    @Deprecated
    private BigDecimal todayTotal;

    private RangedSalesReport.TotalSales totalSales;

    private List<RangedSalesReport.SalesByRange> salesByRange;

    private List<RangedSalesReport.SalesByProduct> salesByProducts;

    private List<RangedSalesReport.SalesByLabel> salesByLabels;
}
