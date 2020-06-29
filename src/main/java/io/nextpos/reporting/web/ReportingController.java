package io.nextpos.reporting.web;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.reporting.data.*;
import io.nextpos.reporting.service.ReportingService;
import io.nextpos.reporting.service.SalesReportService;
import io.nextpos.reporting.service.StatsReportService;
import io.nextpos.reporting.web.model.*;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.service.TimeCardReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    private final SalesReportService salesReportService;

    private final StatsReportService statsReportService;

    private final TimeCardReportService timeCardReportService;

    private final ShiftService shiftService;

    @Autowired
    public ReportingController(final ReportingService reportingService, final SalesReportService salesReportService, final StatsReportService statsReportService, final TimeCardReportService timeCardReportService, final ShiftService shiftService) {
        this.reportingService = reportingService;
        this.salesReportService = salesReportService;
        this.statsReportService = statsReportService;
        this.timeCardReportService = timeCardReportService;
        this.shiftService = shiftService;
    }

    @GetMapping("/rangedSalesReport")
    public RangedSalesReportResponse getRangedSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                          @RequestParam(value = "rangeType", defaultValue = "WEEK") DateParameterType dateParameterType,
                                                          @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                          @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                          @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        final ZonedDateRangeBuilder builder = ZonedDateRangeBuilder.builder(client, dateParameterType)
                .date(date)
                .dateRange(fromDate, toDate);

        shiftService.getMostRecentShift(client.getId()).ifPresent(builder::shift);

        final ZonedDateRange zonedDateRange = builder.build();

        final RangedSalesReport rangedSalesReport = salesReportService.generateRangedSalesReport(client.getId(), zonedDateRange);

        return new RangedSalesReportResponse(
                rangedSalesReport.getDateRange(),
                rangedSalesReport.getTotalSales().getSalesTotal(),
                rangedSalesReport.getTotalSales(),
                rangedSalesReport.getSalesByRange(),
                rangedSalesReport.getSalesByProduct(),
                rangedSalesReport.getSalesByLabel());
    }

    @GetMapping("/salesRankingReport")
    public RangedSalesReportResponse getSalesRankingReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @RequestParam(value = "rangeType", defaultValue = "WEEK") DateParameterType dateParameterType,
                                                           @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                           @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                           @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                           @RequestParam(name = "labelId") String labelId) {

        final ZonedDateRangeBuilder builder = ZonedDateRangeBuilder.builder(client, dateParameterType)
                .date(date)
                .dateRange(fromDate, toDate);

        shiftService.getMostRecentShift(client.getId()).ifPresent(builder::shift);

        final ZonedDateRange zonedDateRange = builder.build();

        final RangedSalesReport rangedSalesReport = salesReportService.generateSalesRankingReport(client.getId(), zonedDateRange, labelId);

        return new RangedSalesReportResponse(
                rangedSalesReport.getDateRange(),
                rangedSalesReport.getTotalSales().getSalesTotal(),
                rangedSalesReport.getTotalSales(),
                rangedSalesReport.getSalesByRange(),
                rangedSalesReport.getSalesByProduct(),
                rangedSalesReport.getSalesByLabel());
    }

    @GetMapping("/customerStats")
    public CustomerStatsReportResponse getCustomerStatsReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                              @RequestParam(name = "year", required = false) Integer year,
                                                              @RequestParam(name = "month", required = false) Integer month) {

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

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH)
                .date(yearMonth.atDay(1)).build();

        final CustomerTrafficReport customerTrafficReport = statsReportService.generateCustomerTrafficReport(client.getId(), zonedDateRange);

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
        final SalesDistribution salesDistribution = salesReportService.generateSalesDistribution(client.getId(), client.getZoneId(), today);
        final SalesDistribution salesDistributionLastYear = salesReportService.generateSalesDistribution(client.getId(), client.getZoneId(), today.minusYears(1));

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
    
    @GetMapping("/averageDeliveryTime")
    public OrderStateAverageTimeReportResponse getOrderStateAverageTimeReport(
            @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
            @RequestParam(name = "date", defaultValue = "MONTH") DateParameterType dateParameterType,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, dateParameterType)
                .dateRange(fromDate, toDate).build();

        final OrderStateParameter orderStateParameter = new OrderStateParameter(zonedDateRange, Order.OrderState.OPEN, Order.OrderState.DELIVERED);
        final OrderStateAverageTimeReport report = reportingService.generateStateTransitionAverageTimeReport(client, orderStateParameter);

        return toOrderStateAverageTimeReportResponse(report, orderStateParameter);
    }

    private OrderStateAverageTimeReportResponse toOrderStateAverageTimeReportResponse(final OrderStateAverageTimeReport report, final OrderStateParameter orderStateParameter) {

        return new OrderStateAverageTimeReportResponse(report.getId(),
                orderStateParameter.getZonedDateRange(),
                report.getFromState(),
                report.getToState(),
                report.getAverageWaitTime() / 1000);
    }
}
