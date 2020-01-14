package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.CustomerCountReport;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
    public CustomerCountReport generateCustomerCountReport(final String clientId, LocalDate dateFilter) {

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("demographicData.male").as("male")
                .and("demographicData.female").as("female")
                .and("demographicData.kid").as("kid")
                .andExpression("demographicData.male + demographicData.female + demographicData.kid").as("customerCount")
                .and("modifiedDate").as("modifiedDate")
                .and("modifiedDate").extractDayOfMonth().as("day");

        LocalDate fromDate = dateFilter.withDayOfMonth(1);
        LocalDate toDate = dateFilter.plusMonths(1).withDayOfMonth(1);
        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(fromDate).lt(toDate));

        final LocalDate lastDayOfMonth = dateFilter.with(TemporalAdjusters.lastDayOfMonth());
        final Integer[] daysOfMonth = IntStream.rangeClosed(1, lastDayOfMonth.getDayOfMonth() + 1).boxed().toArray(Integer[]::new);
        final BucketOperation groupedCustomerCount = Aggregation.bucket("day").withBoundaries(daysOfMonth).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("male")).as("maleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("female")).as("femaleCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("kid")).as("kidCount")
                .andOutput(AccumulatorOperators.Sum.sumOf("customerCount")).as("customerCount")
                .andOutput(context -> new Document("$first", "$modifiedDate")).as("date");

        final FacetOperation facets = Aggregation
                .facet(groupedCustomerCount).as("groupedCustomerCount");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<CustomerCountReport> results = mongoTemplate.aggregate(aggregations, CustomerCountReport.class);

        return results.getUniqueMappedResult();
    }
}
