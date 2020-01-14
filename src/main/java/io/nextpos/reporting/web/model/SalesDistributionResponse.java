package io.nextpos.reporting.web.model;

import io.nextpos.reporting.data.SalesDistribution;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SalesDistributionResponse {

    private List<SalesDistribution.MonthlySales> salesByMonth;
}
