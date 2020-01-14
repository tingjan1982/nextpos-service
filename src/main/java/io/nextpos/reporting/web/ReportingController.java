package io.nextpos.reporting.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.*;
import io.nextpos.reporting.service.ReportingService;
import io.nextpos.reporting.service.SalesReportService;
import io.nextpos.reporting.service.StatsReportService;
import io.nextpos.reporting.web.model.*;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    private final SalesReportService salesReportService;

    private final StatsReportService statsReportService;

    @Autowired
    public ReportingController(final ReportingService reportingService, final SalesReportService salesReportService, final StatsReportService statsReportService) {
        this.reportingService = reportingService;
        this.salesReportService = salesReportService;
        this.statsReportService = statsReportService;
    }

    @GetMapping("/rangedSalesReport")
    public RangedSalesReportResponse getRangedSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                          @RequestParam(value = "rangeType", defaultValue = "WEEK") RangedSalesReport.RangeType rangeType) {

        final RangedSalesReport rangedSalesReport = salesReportService.generateWeeklySalesReport(client.getId(), rangeType);

        return new RangedSalesReportResponse(rangedSalesReport.getTotalSales().getSalesTotal(),
                rangedSalesReport.getSalesByRange(),
                rangedSalesReport.getSalesByProduct());
    }

    @GetMapping("/customerCount")
    public CustomerCountReportResponse getCustomerCountReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final LocalDate today = LocalDate.now();
        final CustomerCountReport customerCountOfThisMonth = statsReportService.generateCustomerCountReport(client.getId(), today);
        final CustomerCountReport customerCountOfThisMonthLastYear = statsReportService.generateCustomerCountReport(client.getId(), today.minusYears(1));

        return new CustomerCountReportResponse(customerCountOfThisMonth.getGroupedCustomerCount(),
                customerCountOfThisMonthLastYear.getGroupedCustomerCount());
    }

    @GetMapping("/salesDistribution")
    public SalesDistributionResponse getSalesDistributionReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final LocalDate today = LocalDate.now();
        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution(client.getId(), today);
        final SalesDistribution salesDistributionLastYear = salesReportService.generateSalesDistribution(client.getId(), today.minusYears(1));

        return new SalesDistributionResponse(salesDistribution.getSalesByMonth(),
                salesDistributionLastYear.getSalesByMonth());
    }

    @GetMapping("/salesreport")
    public SalesReportResponse getSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @RequestParam(name = "date", defaultValue = "TODAY") DateParameterType dateParameterType,
                                              @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
                                              @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {

        ReportDateParameter reportDateParameter;

        if (dateParameterType == DateParameterType.RANGE) {
            reportDateParameter = dateParameterType.toReportingParameter(fromDate, toDate);
        } else {
            reportDateParameter = dateParameterType.toReportingParameter();
        }

        final SalesReport salesReport = reportingService.generateSalesReport(client, reportDateParameter);
        return toSalesReportResponse(salesReport);
    }

    private SalesReportResponse toSalesReportResponse(final SalesReport salesReport) {

        final Map<String, BigDecimal> salesByProducts = salesReport.getSalesByProducts().stream()
                .map(doc -> Pair.of(doc.get("name", String.class), doc.get("amount", Decimal128.class).bigDecimalValue()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return new SalesReportResponse(salesReport.getId(), salesReport.getFromDate(), salesReport.getToDate(), salesReport.getOrderCount(), salesReport.getSalesTotal(), salesByProducts);
    }

    @GetMapping("/averageDeliveryTime")
    public OrderStateAverageTimeReportResponse getOrderStateAverageTimeReport(
            @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
            @RequestParam(name = "date", defaultValue = "TODAY") DateParameterType dateParameterType,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {

        ReportDateParameter reportDateParameter;

        if (dateParameterType == DateParameterType.RANGE) {
            reportDateParameter = dateParameterType.toReportingParameter(fromDate, toDate);
        } else {
            reportDateParameter = dateParameterType.toReportingParameter();
        }

        final OrderStateParameter orderStateParameter = new OrderStateParameter(reportDateParameter, Order.OrderState.OPEN, Order.OrderState.DELIVERED);
        final OrderStateAverageTimeReport report = reportingService.generateStateTransitionAverageTimeReport(client, orderStateParameter);

        return toOrderStateAverageTimeReportResponse(report, orderStateParameter);
    }

    private OrderStateAverageTimeReportResponse toOrderStateAverageTimeReportResponse(final OrderStateAverageTimeReport report, final OrderStateParameter orderStateParameter) {

        return new OrderStateAverageTimeReportResponse(report.getId(),
                orderStateParameter.getDateParameter().getFromDate(),
                orderStateParameter.getDateParameter().getToDate(),
                report.getFromState(),
                report.getToState(),
                report.getAverageWaitTime() / 1000);
    }
}
