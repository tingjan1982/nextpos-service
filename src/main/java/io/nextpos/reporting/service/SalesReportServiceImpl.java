package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.stream.IntStream;

@Service
public class SalesReportServiceImpl implements SalesReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public SalesReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SalesProgress generateSalesProgress(final String clientId) {

        final int today = LocalDate.now().getDayOfMonth();
        final int currentWeek = LocalDate.now().get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfYear());
        final int currentMonth = LocalDate.now().getMonthValue();

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total") // this is critical to make $sum work.
                .and("modifiedDate").as("modifiedDate")
                .and("modifiedDate").extractDayOfMonth().as("day")
                .and("modifiedDate").extractWeek().as("week")
                .and("modifiedDate").extractMonth().as("month");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(LocalDate.now().withDayOfMonth(1)).lt(LocalDate.now().plusMonths(1).withDayOfMonth(1)));

                //.and("month").is(currentMonth));

        final BucketOperation dailySales = Aggregation.bucket("day").withBoundaries(today, today + 1).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$day")).as("dayOfMonth");

        final BucketOperation weeklySales = Aggregation.bucket("week").withBoundaries(currentWeek, currentWeek + 1).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$week")).as("week");

        final BucketOperation monthSales = Aggregation.bucket("month").withBoundaries(1, 13).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$month")).as("month");

        final FacetOperation facets = Aggregation.facet(dailySales).as("dailySales")
                .and(weeklySales).as("weeklySales")
                .and(monthSales).as("monthlySales");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<SalesProgress> result = mongoTemplate.aggregate(aggregations, SalesProgress.class);

        return result.getUniqueMappedResult();
    }

    @Override
    public SalesDistribution generateSalesDistribution(final String clientId) {

        final int currentYear = LocalDate.now().getYear();

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total") // this is critical to make $sum work.
                .and("modifiedDate").as("modifiedDate")
                .and("modifiedDate").extractWeek().as("week")
                .and("modifiedDate").extractMonth().as("month")
                .and("modifiedDate").extractYear().as("year");

        final LocalDate firstDayOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        final LocalDate firstDayOfNextYear = LocalDate.now().with(TemporalAdjusters.firstDayOfNextYear());

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(firstDayOfYear).lt(firstDayOfNextYear));
                        //.and("year").is(currentYear));

        final Integer[] weeks = IntStream.rangeClosed(0, 53).boxed().toArray(Integer[]::new);

        final BucketOperation salesByWeek = Aggregation.bucket("week").withBoundaries(weeks).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$week")).as("week");

        final BucketOperation salesByMonth = Aggregation.bucket("month").withBoundaries(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$month")).as("month");

        final FacetOperation facets = Aggregation.facet(salesByMonth).as("salesByMonth")
                .and(salesByWeek).as("salesByWeek");


        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<SalesDistribution> result = mongoTemplate.aggregate(aggregations, SalesDistribution.class);

        return result.getUniqueMappedResult();
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }

}
