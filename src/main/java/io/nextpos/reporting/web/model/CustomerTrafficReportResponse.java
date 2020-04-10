package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.CustomerTrafficReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTrafficReportResponse {

    private CustomerTrafficReport.TotalCount totalCount;

    private List<CustomerTrafficReport.CustomerTraffic> customerTraffics;

    private List<CustomerTrafficReport.OrdersByType> ordersByType;

    private List<CustomerTrafficReport.OrdersByAgeGroup> ordersByAgeGroup;

    private List<CustomerTrafficReport.OrdersByVisitFrequency> ordersByVisitFrequency;
}
