package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordertransaction.data.PaymentMethodTotal;
import io.nextpos.reporting.data.RangedSalesReport;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RangedSalesReportResponse extends BaseDateRangeResponse {

    private RangedSalesReport.TotalSales totalSales;

    private List<RangedSalesReport.SalesByRange> salesByRange;

    private List<PaymentMethodTotal> salesByPaymentMethods;

    private List<RangedSalesReport.SalesByProduct> salesByProducts;

    private List<RangedSalesReport.SalesByLabel> salesByLabels;

    public RangedSalesReportResponse(ZonedDateRange dateRange, RangedSalesReport report) {
        super(dateRange);
        totalSales = report.getTotalSales();
        salesByRange = report.getSalesByRange();
        salesByPaymentMethods = report.getSalesByPaymentMethod();
        salesByProducts = report.getSalesByProduct();
        salesByLabels = report.getSalesByLabel();
    }
}
