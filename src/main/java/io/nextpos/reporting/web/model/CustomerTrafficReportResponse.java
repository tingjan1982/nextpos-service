package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.CustomerTrafficReport;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CustomerTrafficReportResponse {

    private List<CustomerTrafficReport.CustomerTraffic> customerTraffics;
}
