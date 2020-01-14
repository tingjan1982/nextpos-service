package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
public class SalesReportServiceImpl implements SalesReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public SalesReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public RangedSalesReport generateWeeklySalesReport(final String clientId, final RangedSalesReport.RangeType rangeType) {

        ProjectionOperation projection = createProjection(rangeType);
        final UnwindOperation flattenLineItems = Aggregation.unwind("lineItems");
        final MatchOperation filter = createMatchFilter(clientId, rangeType);

        final GroupOperation salesTotal = Aggregation.group("clientId")
                .sum(createToDecimal("lineItems.subTotal.amountWithTax")).as("salesTotal");

        final BucketOperation salesByRange = createSalesByRangeFacet(rangeType);

        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("lineItems.subTotal.amountWithTax");
        final GroupOperation salesByProduct = Aggregation.group("lineItems.productSnapshot.name")
                .sum(subTotalToDecimal).as("productSales")
                .sum("lineItems.quantity").as("salesQuantity")
                .first("lineItems.productSnapshot.name").as("productName");

        final FacetOperation facets = Aggregation
                .facet(salesTotal).as("totalSales")
                .and(salesByRange).as("salesByRange")
                .and(salesByProduct).as("salesByProduct");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                flattenLineItems,
                filter,
                facets);

        final AggregationResults<RangedSalesReport> result = mongoTemplate.aggregate(aggregations, RangedSalesReport.class);

        RangedSalesReport results = result.getUniqueMappedResult();

        if (results != null && results.hasResult()) {
            enhanceResults(results, rangeType);
        } else {
            results = new RangedSalesReport();
        }
        return results;
    }

    private ProjectionOperation createProjection(final RangedSalesReport.RangeType rangeType) {
        ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total") // this is critical to make $sum work.
                .and("orderLineItems").as("lineItems")
                .and("modifiedDate").as("modifiedDate");

        switch (rangeType) {
            case WEEK:
                projection = projection.and("modifiedDate").extractDayOfWeek().as("day");
                break;
            case MONTH:
                projection = projection.and("modifiedDate").extractDayOfMonth().as("day");
                break;
        }
        return projection;
    }

    private MatchOperation createMatchFilter(final String clientId, final RangedSalesReport.RangeType rangeType) {

        LocalDate fromDate;
        LocalDate toDate;

        switch (rangeType) {
            case WEEK:
                final TemporalField temporalField = WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek();
                final int todayOfWeek = LocalDate.now().get(ChronoField.DAY_OF_WEEK);

                final LocalDate firstDayOfCurrentWeek = LocalDate.now().with(temporalField, 1);
                final LocalDate firstDayOfNextWeek = LocalDate.now().with(temporalField, 7).plusDays(1);
                fromDate = firstDayOfCurrentWeek;
                toDate = firstDayOfNextWeek;
                break;
            case MONTH:
                fromDate = LocalDate.now().withDayOfMonth(1);
                toDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
                break;
            default:
                throw new GeneralApplicationException("Ensure all RangeType is supported: " + Arrays.toString(RangedSalesReport.RangeType.values()));
        }

        return Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(fromDate).lt(toDate));
    }

    private BucketOperation createSalesByRangeFacet(RangedSalesReport.RangeType rangeType) {

        switch (rangeType) {
            case WEEK:
                final Integer[] daysInWeek = IntStream.rangeClosed(1, 8).boxed().toArray(Integer[]::new);

                return Aggregation.bucket("day").withBoundaries(daysInWeek).withDefaultBucket("Other")
                        .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                        .andOutput(context -> new Document("$first", "$modifiedDate")).as("date");

            case MONTH:
                final LocalDate lastDayOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
                final Integer[] dayOfMonth = IntStream.rangeClosed(1, lastDayOfMonth.getDayOfMonth() + 1).boxed().toArray(Integer[]::new);

                return Aggregation.bucket("day").withBoundaries(dayOfMonth).withDefaultBucket("Other")
                        .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                        .andOutput(context -> new Document("$first", "$modifiedDate")).as("date");

            default:
                throw new GeneralApplicationException("Ensure all RangeType is supported: " + Arrays.toString(RangedSalesReport.RangeType.values()));
        }
    }

    private void enhanceResults(final RangedSalesReport results, final RangedSalesReport.RangeType rangeType) {

        results.setRangeType(rangeType);
        final BigDecimal salesTotal = results.getTotalSales().getSalesTotal();

        results.getSalesByRange().forEach(s -> {
            final LocalDate date = s.getDate();
            s.setFormattedDate(date.format(DateTimeFormatter.ofPattern("E MM/dd").withLocale(Locale.TAIWAN)));
        });


        results.getSalesByProduct().forEach(s -> {
            final BigDecimal productSales = s.getProductSales();
            final BigDecimal percentage = productSales.multiply(BigDecimal.valueOf(100)).divide(salesTotal, RoundingMode.UP);
            s.setPercentage(percentage);
        });
    }

    @Override
    public SalesProgress generateSalesProgress(final String clientId) {

        final int today = LocalDate.now().getDayOfMonth();
        final int currentWeek = LocalDate.now().get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfYear());

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total") // this is critical to make $sum work.
                .and("modifiedDate").as("modifiedDate")
                .and("modifiedDate").extractDayOfMonth().as("day")
                .and("modifiedDate").extractWeek().as("week")
                .and("modifiedDate").extractMonth().as("month");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(LocalDate.now().withDayOfMonth(1)).lt(LocalDate.now().plusMonths(1).withDayOfMonth(1)));

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

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and(createToDecimal("total.amountWithTax")).as("total") // this is critical to make $sum work.
                .and("modifiedDate").as("modifiedDate")
                .and("modifiedDate").extractWeek().as("week")
                .and("modifiedDate").extractMonth().as("month");

        final LocalDate firstDayOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        final LocalDate firstDayOfNextYear = LocalDate.now().with(TemporalAdjusters.firstDayOfNextYear());

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("modifiedDate").gte(firstDayOfYear).lt(firstDayOfNextYear));

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
