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

    private BigDecimal todayTotal;

    private List<RangedSalesReport.SalesByRange> salesByRange;

    private List<RangedSalesReport.SalesByProduct> salesByProducts;
}
