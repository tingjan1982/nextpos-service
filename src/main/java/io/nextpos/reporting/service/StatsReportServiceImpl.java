package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.CustomerStatsReport;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class StatsReportServiceImpl implements StatsReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public StatsReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public CustomerStatsReport generateCustomerStatsReport(final String clientId, YearMonth dateFilter) {

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total")
                .and("demographicData.male").as("male")
                .and("demographicData.female").as("female")
                .and("demographicData.kid").as("kid")
                .andExpression("demographicData.male + demographicData.female + demographicData.kid").as("customerCount")
                //.and(context -> Document.parse("{ $divide: [ { $toDecimal: [ '$total.amountWithTax' ] }, { $add: [ '$demographicData.male', '$demographicData.female', '$demographicData.kid' ] } ] }")).as("avgCustomerSpending")
                .and("modifiedDate").as("modifiedDate")
                .and(context -> Document.parse("{ $dayOfMonth: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("day");

        LocalDate fromDate = dateFilter.atDay(1);
        LocalDate toDate = dateFilter.plusMonths(1).atDay(1);
        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(fromDate).lt(toDate));

        final LocalDate lastDayOfMonth = dateFilter.atEndOfMonth();
        final Integer[] daysOfMonth = IntStream.rangeClosed(1, lastDayOfMonth.getDayOfMonth() + 1).boxed().toArray(Integer[]::new);
        final BucketOperation groupedCustomerStats = Aggregation.bucket("day").withBoundaries(daysOfMonth).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(AccumulatorOperators.Sum.sumOf("male")).as("maleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("female")).as("femaleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("kid")).as("kidCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("customerCount")).as("customerCount")
                .andOutput(context -> new Document("$first", "$modifiedDate")).as("date");

        final ProjectionOperation test = Aggregation.project("total", "maleCount", "femaleCount", "kidCount", "customerCount", "date")
                .and(context -> Document.parse("{ $cond: [ {$gt: ['$customerCount', 0]}, {$divide: ['$total', '$customerCount']}, '$total'] }")).as("averageSpending");

        final FacetOperation facets = Aggregation.facet(groupedCustomerStats, test).as("groupedCustomerStats");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<CustomerStatsReport> results = mongoTemplate.aggregate(aggregations, CustomerStatsReport.class);
        CustomerStatsReport customerStatsReport = results.getUniqueMappedResult();

        if (customerStatsReport == null) {
            customerStatsReport = new CustomerStatsReport();
        } else {
            enhanceResult(customerStatsReport, dateFilter);
        }

        return customerStatsReport;
    }

    private void enhanceResult(final CustomerStatsReport customerStatsReport, final YearMonth dateFilter) {

        final int lastDayOfMonth = dateFilter.atEndOfMonth().getDayOfMonth();

        if (customerStatsReport.getGroupedCustomerStats().size() != lastDayOfMonth) {
            final Map<String, CustomerStatsReport.CustomerStats> statsMap = customerStatsReport.getGroupedCustomerStats().stream()
                    .collect(Collectors.toMap(CustomerStatsReport.CustomerStats::getId, s -> s));

            final ArrayList<CustomerStatsReport.CustomerStats> enhancedCustomerStatsList = new ArrayList<>();

            for (int i = 1; i <= lastDayOfMonth; i++) {
                final String day = String.valueOf(i);

                if (statsMap.containsKey(day)) {
                    enhancedCustomerStatsList.add(statsMap.get(day));
                } else {
                    final CustomerStatsReport.CustomerStats zeroCustomerStats = new CustomerStatsReport.CustomerStats();
                    zeroCustomerStats.setId(day);
                    zeroCustomerStats.setDate(dateFilter.atDay(i));

                    enhancedCustomerStatsList.add(zeroCustomerStats);
                }
            }

            customerStatsReport.setGroupedCustomerStats(enhancedCustomerStatsList);
        }
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }
}
