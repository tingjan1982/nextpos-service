package io.nextpos.reporting.service;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.CustomerStatsReport;
import io.nextpos.reporting.data.CustomerTrafficReport;
import io.nextpos.reporting.data.ReportEnhancer;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@MongoTransaction
public class StatsReportServiceImpl implements StatsReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public StatsReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public CustomerTrafficReport generateCustomerTrafficReport(String clientId, ZonedDateRange zonedDateRange) {

        final String timezone = zonedDateRange.getClientTimeZone().getId();

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and("orderType").as("orderType")
                .and("demographicData.ageGroup").as("ageGroup")
                .and("demographicData.visitFrequency").as("visitFrequency")
                .and("demographicData.male").as("male")
                .and("demographicData.female").as("female")
                .and("demographicData.kid").as("kid")
                .andExpression("demographicData.male + demographicData.female + demographicData.kid").as("customerCount")
                .and(context -> Document.parse("{ $hour: {date: '$createdDate', timezone: '" + timezone + "'} }")).as("hour")
                .and("createdDate").as("createdDate");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(zonedDateRange.getFromDate()).lt(zonedDateRange.getToDate()));

        final GroupOperation counts = Aggregation.group("clientId")
                .sum("male").as("maleCount")
                .sum("female").as("femaleCount")
                .sum("kid").as("kidCount")
                .sum("customerCount").as("customerCount")
                .count().as("orderCount");

        final Object[] hoursOfDay = IntStream.rangeClosed(0, 24).boxed().toArray(Integer[]::new);
        final BucketOperation ordersByHour = Aggregation.bucket("hour").withBoundaries(hoursOfDay).withDefaultBucket("Other")
                .andOutputCount().as("orderCount")
                .andOutput("customerCount").sum().as("customerCount")
                .andOutput(context -> new Document("$first", "$hour")).as("hourOfDay");

        final GroupOperation ordersByType = Aggregation.group("orderType")
                .first("orderType").as("orderType")
                .count().as("orderCount");

        final GroupOperation ordersByAgeGroup = Aggregation.group("ageGroup")
                .first(ConditionalOperators.ifNull("ageGroup").then(Order.DemographicData.AgeGroup.NOT_ENTERED)).as("ageGroup")
                .count().as("orderCount");

        final GroupOperation ordersByVisitFrequency = Aggregation.group("visitFrequency")
                .first(ConditionalOperators.ifNull("visitFrequency").then(Order.DemographicData.VisitFrequency.NOT_ENTERED)).as("visitFrequency")
                .count().as("orderCount");

        final FacetOperation facets = Aggregation.facet(counts).as("counts")
                .and(ordersByHour).as("ordersByHour")
                .and(ordersByType).as("ordersByType")
                .and(ordersByAgeGroup).as("ordersByAgeGroup")
                .and(ordersByVisitFrequency).as("ordersByVisitFrequency");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<CustomerTrafficReport> results = mongoTemplate.aggregate(aggregations, CustomerTrafficReport.class);
        final CustomerTrafficReport customerTrafficReport = results.getUniqueMappedResult();

        if (customerTrafficReport != null) {
            customerTrafficReport.enhanceResults();
        }

        return customerTrafficReport;
    }

    @Override
    public CustomerStatsReport generateCustomerStatsReport(final String clientId, ZonedDateRange zonedDateRange) {

        final String timezone = zonedDateRange.getClientTimeZone().getId();

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("total")
                .and("demographicData.male").as("male")
                .and("demographicData.female").as("female")
                .and("demographicData.kid").as("kid")
                .andExpression("demographicData.male + demographicData.female + demographicData.kid").as("customerCount")
                //.and(context -> Document.parse("{ $divide: [ { $toDecimal: [ '$total.amountWithTax' ] }, { $add: [ '$demographicData.male', '$demographicData.female', '$demographicData.kid' ] } ] }")).as("avgCustomerSpending")
                .and("createdDate").as("createdDate")
                .and(context -> Document.parse("{ $dayOfMonth: {date: '$createdDate', timezone: '" + timezone + "'} }")).as("day");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(zonedDateRange.getFromDate()).lt(zonedDateRange.getToDate()));

        final LocalDate lastDayOfMonth = zonedDateRange.getZonedToDate().with(TemporalAdjusters.lastDayOfMonth()).toLocalDate();
        final Object[] daysOfMonth = IntStream.rangeClosed(1, lastDayOfMonth.getDayOfMonth() + 1).boxed().toArray(Integer[]::new);

        final BucketOperation groupedCustomerStats = Aggregation.bucket("day").withBoundaries(daysOfMonth).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(AccumulatorOperators.Sum.sumOf("male")).as("maleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("female")).as("femaleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("kid")).as("kidCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("customerCount")).as("customerCount")
                .andOutput(context -> new Document("$first", "$createdDate")).as("date");

        final ProjectionOperation test = Aggregation.project("total", "maleCount", "femaleCount", "kidCount", "customerCount", "date")
                .and(context -> Document.parse("{ $cond: [ {$gt: ['$customerCount', 0]}, {$divide: ['$total', '$customerCount']}, '$total'] }")).as("averageSpending");

        final FacetOperation facets = Aggregation.facet(groupedCustomerStats, test).as("groupedCustomerStats");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<CustomerStatsReport> results = mongoTemplate.aggregate(aggregations, CustomerStatsReport.class);
        final CustomerStatsReport customerStatsReport = results.getUniqueMappedResult();

        if (customerStatsReport != null) {
            ReportEnhancer.enhanceReportResult(IntStream.rangeClosed(1, lastDayOfMonth.getDayOfMonth()),
                    () -> customerStatsReport.getGroupedCustomerStats().stream().collect(Collectors.toMap(CustomerStatsReport.CustomerStats::getId, s -> s)),
                    (id) -> CustomerStatsReport.CustomerStats.emptyObject(id, lastDayOfMonth.withDayOfMonth(Integer.parseInt(id))),
                    customerStatsReport::setGroupedCustomerStats);
        }

        return customerStatsReport;
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }
}
