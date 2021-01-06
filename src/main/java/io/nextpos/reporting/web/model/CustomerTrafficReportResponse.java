package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.reporting.data.CustomerTrafficReport;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerTrafficReportResponse extends BaseDateRangeResponse {

    private CustomerTrafficReport.TotalCount totalCount;

    private List<CustomerTrafficReport.CustomerTraffic> customerTraffics;

    private List<CustomerTrafficReport.OrdersByType> ordersByType;

    private List<CustomerTrafficReport.OrdersByAgeGroup> ordersByAgeGroup;

    private List<CustomerTrafficReport.OrdersByVisitFrequency> ordersByVisitFrequency;

    public CustomerTrafficReportResponse(ZonedDateRange dateRange, CustomerTrafficReport customerTrafficReport) {
        super(dateRange);

        customerTrafficReport.getTotalCountObject().ifPresentOrElse(tc -> {
            this.totalCount = tc;
            this.customerTraffics = customerTrafficReport.getOrdersByHour();
            this.ordersByType = customerTrafficReport.getOrdersByType();
            this.ordersByAgeGroup = customerTrafficReport.getOrdersByAgeGroup();
            this.ordersByVisitFrequency = customerTrafficReport.getOrdersByVisitFrequency();

            }, () -> this.totalCount = new CustomerTrafficReport.TotalCount());
    }


}
