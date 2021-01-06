package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.reporting.data.CustomerStatsReport;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerStatsReportResponse extends BaseDateRangeResponse {

    private List<CustomerStatsReport.CustomerStats> customerStatsThisMonth;

    @Deprecated
    private List<CustomerStatsReport.CustomerStats> customerStatsThisMonthLastYear;

    public CustomerStatsReportResponse(ZonedDateRange dateRange, List<CustomerStatsReport.CustomerStats> customerStatsThisMonth, List<CustomerStatsReport.CustomerStats> customerStatsThisMonthLastYear) {
        super(dateRange);
        this.customerStatsThisMonth = customerStatsThisMonth;
        this.customerStatsThisMonthLastYear = customerStatsThisMonthLastYear;
    }
}
