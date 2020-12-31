package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordertransaction.data.PaymentMethodTotal;
import io.nextpos.reporting.data.RangedSalesReport;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RangedSalesReportResponse {

    private ZonedDateRange dateRange;

    private RangedSalesReport.TotalSales totalSales;

    private List<RangedSalesReport.SalesByRange> salesByRange;

    private List<PaymentMethodTotal> salesByPaymentMethods;

    private List<RangedSalesReport.SalesByProduct> salesByProducts;

    private List<RangedSalesReport.SalesByLabel> salesByLabels;

    public RangedSalesReportResponse(RangedSalesReport report) {
        dateRange = report.getDateRange();
        totalSales = report.getTotalSales();
        salesByRange = report.getSalesByRange();
        salesByPaymentMethods = report.getSalesByPaymentMethod();
        salesByProducts = report.getSalesByProduct();
        salesByLabels = report.getSalesByLabel();
    }
}
