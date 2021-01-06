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

        final ZonedDateRange zonedDateRange = createZonedDateRange(client, dateParameterType, date, fromDate, toDate);

        final RangedSalesReport rangedSalesReport = salesReportService.generateRangedSalesReport(client.getId(), zonedDateRange);

        return new RangedSalesReportResponse(zonedDateRange, rangedSalesReport);
    }

    @GetMapping("/salesRankingReport")
    public RangedSalesReportResponse getSalesRankingReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @RequestParam(value = "rangeType", defaultValue = "WEEK") DateParameterType dateParameterType,
                                                           @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                           @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                           @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                           @RequestParam(name = "labelId") String labelId) {

        final ZonedDateRange zonedDateRange = createZonedDateRange(client, dateParameterType, date, fromDate, toDate);

        final RangedSalesReport rangedSalesReport = salesReportService.generateSalesRankingReport(client.getId(), zonedDateRange, labelId);

        return new RangedSalesReportResponse(zonedDateRange, rangedSalesReport);
    }

    @GetMapping(value = "/customerStats", headers = "version=v2")
    public CustomerStatsReportResponse getCustomerStatsReportV2(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                @RequestParam(value = "rangeType") DateParameterType dateParameterType,
                                                                @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                                @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        final ZonedDateRange zonedDateRange = createZonedDateRange(client, dateParameterType, null, fromDate, toDate);

        final CustomerStatsReport customerStatsOfThisMonth = statsReportService.generateCustomerStatsReport(client.getId(), zonedDateRange);

        return new CustomerStatsReportResponse(zonedDateRange, customerStatsOfThisMonth.getGroupedCustomerStats(), null);
    }

    @GetMapping("/customerStats")
    @Deprecated
    public CustomerStatsReportResponse getCustomerStatsReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                              @RequestParam(name = "year", required = false) Integer year,
                                                              @RequestParam(name = "month", required = false) Integer month) {

        YearMonth yearMonth = YearMonth.now();

        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH)
                .date(yearMonth.atDay(1)).build();

        final CustomerStatsReport customerStatsOfThisMonth = statsReportService.generateCustomerStatsReport(client.getId(), zonedDateRange);

        final ZonedDateRange zonedDateRangeLastYear = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH)
                .date(yearMonth.atDay(1).minusYears(1)).build();

        final CustomerStatsReport customerStatsOfThisMonthLastYear = statsReportService.generateCustomerStatsReport(client.getId(), zonedDateRangeLastYear);

        return new CustomerStatsReportResponse(
                zonedDateRange,
                customerStatsOfThisMonth.getGroupedCustomerStats(),
                customerStatsOfThisMonthLastYear.getGroupedCustomerStats());
    }

    @GetMapping(value = "/customerTraffic", headers = "version=v2")
    public CustomerTrafficReportResponse getCustomerTrafficReportV2(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                    @RequestParam(value = "rangeType") DateParameterType dateParameterType,
                                                                    @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                                    @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        final ZonedDateRange zonedDateRange = createZonedDateRange(client, dateParameterType, null, fromDate, toDate);
        final CustomerTrafficReport customerTrafficReport = statsReportService.generateCustomerTrafficReport(client.getId(), zonedDateRange);

        return new CustomerTrafficReportResponse(zonedDateRange, customerTrafficReport);
    }

    private ZonedDateRange createZonedDateRange(Client client, DateParameterType dateParameterType, LocalDate date, LocalDateTime fromDate, LocalDateTime toDate) {

        final ZonedDateRangeBuilder builder = ZonedDateRangeBuilder.builder(client, dateParameterType)
                .date(date)
                .dateRange(fromDate, toDate);

        shiftService.getMostRecentShift(client.getId()).ifPresent(builder::shift);

        return builder.build();
    }

    @GetMapping("/customerTraffic")
    @Deprecated
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

        return new CustomerTrafficReportResponse(zonedDateRange, customerTrafficReport);
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
                                            @RequestParam(name = "month", required = false) Integer month) {

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
