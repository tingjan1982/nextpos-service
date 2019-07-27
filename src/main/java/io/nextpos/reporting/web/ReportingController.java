package io.nextpos.reporting.web;

import io.nextpos.client.data.Client;
import io.nextpos.reporting.data.ReportingParameter;
import io.nextpos.reporting.data.SalesReport;
import io.nextpos.reporting.service.ReportingService;
import io.nextpos.reporting.web.model.SalesReportResponse;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    @Autowired
    public ReportingController(final ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/salesreport")
    public SalesReportResponse getSalesReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @RequestParam(name = "date", defaultValue = "TODAY") DateParameter dateParameter,
                                              @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
                                              @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {

        ReportingParameter reportingParameter;

        if (dateParameter == DateParameter.RANGE) {
            reportingParameter = dateParameter.toReportingParameter(fromDate, toDate);
        } else {
            reportingParameter = dateParameter.toReportingParameter();
        }

        final SalesReport salesReport = reportingService.generateSalesReport(client, reportingParameter);
        return toSalesReportResponse(salesReport);
    }

    private SalesReportResponse toSalesReportResponse(final SalesReport salesReport) {

        final Map<String, BigDecimal> salesByProducts = salesReport.getSalesByProducts().stream()
                .map(doc -> Pair.of(doc.get("name", String.class), doc.get("amount", Decimal128.class).bigDecimalValue()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return new SalesReportResponse(salesReport.getId(), salesReport.getFromDate(), salesReport.getToDate(), salesReport.getSalesTotal(), salesByProducts);
    }

    public enum DateParameter {
        TODAY {
            @Override
            public ReportingParameter toReportingParameter() {

                final LocalDateTime startOfDay = LocalDate.now().atTime(8, 0);
                final LocalDateTime endOfDay = startOfDay.plusDays(1);

                return new ReportingParameter(startOfDay, endOfDay);
            }
        },

        WEEK {
            @Override
            public ReportingParameter toReportingParameter() {

                final LocalDateTime startOfWeek = LocalDate.now().atTime(8, 0).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                final LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

                return new ReportingParameter(startOfWeek, endOfWeek);
            }
        },

        MONTH {
            @Override
            public ReportingParameter toReportingParameter() {

                final LocalDateTime startOfMonth = LocalDate.now().atTime(8, 0).with(TemporalAdjusters.firstDayOfMonth());
                final LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

                return new ReportingParameter(startOfMonth, endOfMonth);
            }
        },

        RANGE {
            @Override
            public ReportingParameter toReportingParameter() {
                throw new UnsupportedOperationException("The parameterized variant of this method should be used instead.");
            }
        };

        public abstract ReportingParameter toReportingParameter();

        public ReportingParameter toReportingParameter(Date fromDate, Date toDate) {

            if (fromDate != null && toDate != null) {
                final LocalDateTime fromDT = LocalDate.ofInstant(fromDate.toInstant(), ZoneId.systemDefault()).atTime(8, 0);
                final LocalDateTime toDT = LocalDate.ofInstant(toDate.toInstant(), ZoneId.systemDefault()).atTime(8, 0);

                return new ReportingParameter(fromDT, toDT);
            }

            return TODAY.toReportingParameter();
        }
    }
}
