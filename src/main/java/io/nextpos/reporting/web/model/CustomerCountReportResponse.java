package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.CustomerCountReport;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CustomerCountReportResponse {

    private List<CustomerCountReport.CustomerCount> customerCountThisMonth;

    private List<CustomerCountReport.CustomerCount> customerCountThisMonthLastYear;
}
