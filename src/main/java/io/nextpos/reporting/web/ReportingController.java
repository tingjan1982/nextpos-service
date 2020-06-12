package io.nextpos.reporting.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.*;
import io.nextpos.reporting.service.ReportingService;
import io.nextpos.reporting.service.SalesReportService;
import io.nextpos.reporting.service.StatsReportService;
import io.nextpos.reporting.web.model.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.service.TimeCardReportService;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    private final SalesReportService salesReportService;

    private final StatsReportService statsReportService;

    private final TimeCardReportService timeCardReportService;

    @Autowired
    public ReportingController(final ReportingService reportingService, final SalesReportService salesReportService, final StatsReportService statsReportService, final TimeCardReportService timeCardReportService) {
        this.reportingService = reportingService;
        this.salesReportService = salesReportService;
        this.statsReportService = statsReportService;
        this.timeCardReportService = timeCardReportService;
    }

    @GetMapping("/rangedSalesReport")
    public RangedSalesReportResponse getRangedSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                          @RequestParam(value = "rangeType", defaultValue = "WEEK") RangedSalesReport.RangeType rangeType,
                                                          @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                          @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                          @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        if (date == null) {
            date = LocalDate.now();
        }

        if (rangeType == RangedSalesReport.RangeType.CUSTOM && (fromDate == null || toDate == null)) {
            throw new BusinessLogicException("'from' and 'to' date parameter must be specified for CUSTOM range type");
        }

        if (fromDate.isAfter(toDate)) {
            throw new BusinessLogicException("from date cannot be after to date");
        }

        ReportDateParameter reportDateParameter = new ReportDateParameter(fromDate, toDate);

        final RangedSalesReport rangedSalesReport = salesReportService.generateRangedSalesReport(client.getId(), rangeType, date, reportDateParameter);

        return new RangedSalesReportResponse(
                date,
                reportDateParameter,
                rangedSalesReport.getTotalSales().getSalesTotal(),
                rangedSalesReport.getSalesByRange(),
                rangedSalesReport.getSalesByProduct());
    }

    @GetMapping("/customerStats")
    public CustomerStatsReportResponse getCustomerStatsReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                              @RequestParam(name = "year", required = false) Integer year,
                                                              @RequestParam(name = "month", required = false) Month month) {

        YearMonth yearMonth = YearMonth.now();

        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }

        final CustomerStatsReport customerStatsOfThisMonth = statsReportService.generateCustomerStatsReport(client.getId(), yearMonth);
        final CustomerStatsReport customerStatsOfThisMonthLastYear = statsReportService.generateCustomerStatsReport(client.getId(), yearMonth.minusYears(1));

        return new CustomerStatsReportResponse(
                customerStatsOfThisMonth.getGroupedCustomerStats(),
                customerStatsOfThisMonthLastYear.getGroupedCustomerStats());
    }

    @GetMapping("/customerTraffic")
    public CustomerTrafficReportResponse getCustomerTrafficReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                  @RequestParam(name = "year", required = false) Integer year,
                                                                  @RequestParam(name = "month", required = false) Integer month) {
        YearMonth yearMonth = YearMonth.now();

        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }

        final CustomerTrafficReport customerTrafficReport = statsReportService.generateCustomerTrafficReport(client.getId(), yearMonth);

        if (customerTrafficReport.getTotalCountObject().isPresent()) {
            return new CustomerTrafficReportResponse(
                    customerTrafficReport.getTotalCountObject().get(),
                    customerTrafficReport.getOrdersByHour(),
                    customerTrafficReport.getOrdersByType(),
                    customerTrafficReport.getOrdersByAgeGroup(),
                    customerTrafficReport.getOrdersByVisitFrequency());
        } else {
            final CustomerTrafficReportResponse empty = new CustomerTrafficReportResponse();
            empty.setTotalCount(new CustomerTrafficReport.TotalCount());

            return empty;
        }

    }

    @GetMapping("/salesDistribution")
    public SalesDistributionResponse getSalesDistributionReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final LocalDate today = LocalDate.now();
        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution(client.getId(), today);
        final SalesDistribution salesDistributionLastYear = salesReportService.generateSalesDistribution(client.getId(), today.minusYears(1));

        return new SalesDistributionResponse(salesDistribution.getSalesByMonth(),
                salesDistributionLastYear.getSalesByMonth());
    }

    @GetMapping("/timeCardReport")
    public TimeCardReport getTimeCardReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @RequestParam(name = "year", required = false) Integer year,
                                            @RequestParam(name = "month", required = false) Month month) {

        YearMonth yearMonth = YearMonth.now();

        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }

        return timeCardReportService.getTimeCardReport(client, yearMonth);
    }

    @GetMapping("/salesreport")
    public SalesReportResponse getSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @RequestParam(name = "date", defaultValue = "TODAY") DateParameterType dateParameterType,
                                              @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
                                              @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {

        ReportDateParameter reportDateParameter = DateParameterType.toReportingParameter(dateParameterType, fromDate, toDate);

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

        ReportDateParameter reportDateParameter = DateParameterType.toReportingParameter(dateParameterType, fromDate, toDate);

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
