package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.CustomerStatsReport;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CustomerStatsReportResponse {

    private List<CustomerStatsReport.CustomerStats> customerStatsThisMonth;

    private List<CustomerStatsReport.CustomerStats> customerStatsThisMonthLastYear;
}
