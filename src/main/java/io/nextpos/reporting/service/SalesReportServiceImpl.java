package io.nextpos.reporting.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.*;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class SalesReportServiceImpl implements SalesReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public SalesReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public RangedSalesReport generateRangedSalesReport(final String clientId,
                                                       final RangedSalesReport.RangeType rangeType,
                                                       final LocalDate date,
                                                       final ReportDateParameter reportDateParameter) {

        final ZonedDateRange zonedDateRange = new ZonedDateRange(ZoneId.of("Asia/Taipei"));

        ProjectionOperation projection = createProjection();
        final MatchOperation filter = createMatchFilter(clientId, rangeType, date, reportDateParameter, zonedDateRange);

        final GroupOperation salesTotal = Aggregation.group("clientId")
                .sum("orderTotal").as("salesTotal");

        final UnwindOperation flattenLineItems = Aggregation.unwind("lineItems");
        final BucketOperationSupport<?, ?> salesByRange = createSalesByRangeFacet(zonedDateRange);

        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("lineItems.lineItemSubTotal");
        final GroupOperation salesByProduct = Aggregation.group("lineItems.productSnapshot.name")
                .sum(subTotalToDecimal).as("productSales")
                .sum("lineItems.quantity").as("salesQuantity")
                .first("lineItems.productSnapshot.name").as("productName");
        final SortOperation sortSalesByProduct = Aggregation.sort(Sort.Direction.DESC, "productSales");

        final FacetOperation facets = Aggregation
                .facet(salesTotal).as("totalSales")
                .and(salesByRange).as("salesByRange")
                .and(flattenLineItems, salesByProduct, sortSalesByProduct).as("salesByProduct");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<RangedSalesReport> result = mongoTemplate.aggregate(aggregations, RangedSalesReport.class);

        RangedSalesReport results = result.getUniqueMappedResult();

        if (results != null) {
            enhanceResults(results, zonedDateRange);
        } else {
            results = new RangedSalesReport();
        }
        return results;
    }

    private ProjectionOperation createProjection() {

        return Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("orderTotal")
                .and("orderLineItems").as("lineItems")
                .and("createdDate").as("createdDate")
                .and(context -> Document.parse("{ $dayOfYear: {date: '$createdDate', timezone: 'Asia/Taipei'} }")).as("dayOfYear");
    }

    private MatchOperation createMatchFilter(String clientId,
                                             RangedSalesReport.RangeType rangeType,
                                             LocalDate date,
                                             ReportDateParameter reportDateParameter,
                                             ZonedDateRange zonedDateRange) {

        final ZoneId zoneId = zonedDateRange.getClientTimeZone();

        switch (rangeType) {
            case WEEK:
                final TemporalField temporalField = WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek();
                final LocalDate firstDayOfCurrentWeek = date.with(temporalField, 1);
                final LocalDate firstDayOfNextWeek = date.with(temporalField, 7).plusDays(1);

                zonedDateRange.setZonedFromDate(firstDayOfCurrentWeek.atStartOfDay(zoneId));
                zonedDateRange.setZonedToDate(firstDayOfNextWeek.atStartOfDay(zoneId));

                break;

            case MONTH:
                zonedDateRange.setZonedFromDate(date.withDayOfMonth(1).atStartOfDay(zoneId));
                zonedDateRange.setZonedToDate(date.plusMonths(1).withDayOfMonth(1).atStartOfDay(zoneId));

                break;

            case CUSTOM:
                zonedDateRange.setZonedFromDate(reportDateParameter.getFromDate().atZone(zoneId));
                zonedDateRange.setZonedToDate(reportDateParameter.getToDate().atZone(zoneId));

                break;

            default:
                throw new GeneralApplicationException("Ensure all RangeType is supported: " + Arrays.toString(RangedSalesReport.RangeType.values()));
        }

        return Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(zonedDateRange.getFromLocalDateTime()).lt(zonedDateRange.getToLocalDateTime()));
    }

    private BucketOperationSupport<?, ?> createSalesByRangeFacet(ZonedDateRange zonedDateRange) {

        final Object[] daysOfYear = zonedDateRange.bucketDateRange().boxed().toArray(Integer[]::new);

        return Aggregation.bucket("dayOfYear").withBoundaries(daysOfYear).withDefaultBucket("Other")
                .andOutput("dayOfYear").last().as("dayOfYear")
                .andOutput("orderTotal").sum().as("total")
                .andOutput(context -> new Document("$last", "$createdDate")).as("date");
    }

    private void enhanceResults(RangedSalesReport results,
                                ZonedDateRange zonedDateRange) {

        results.setDateRange(zonedDateRange);

        ReportEnhancer.enhanceReportResult(zonedDateRange.dateRange(),
                () -> results.getSalesByRange().stream()
                        .collect(Collectors.toMap(s -> String.valueOf(s.getDayOfYear()), s -> s)),
                id -> RangedSalesReport.SalesByRange.emptyObject(id, zonedDateRange.getZonedFromDate().withDayOfYear(Integer.parseInt(id)).toLocalDate()),
                results::setSalesByRange);

        if (results.hasResult()) {
            final BigDecimal salesTotal = results.getTotalSales().getSalesTotal();

            results.getSalesByProduct().forEach(s -> {
                final BigDecimal productSales = s.getProductSales();
                final BigDecimal percentage = productSales.multiply(BigDecimal.valueOf(100)).divide(salesTotal, RoundingMode.UP);
                s.setPercentage(percentage);
            });
        }
    }

    @Override
    public SalesProgress generateSalesProgress(final String clientId) {

        final int today = LocalDate.now().getDayOfMonth();
        final int currentWeek = LocalDate.now().get(WeekFields.of(DayOfWeek.SUNDAY, 7).weekOfYear());

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("total") // this is critical to make $sum work.
                .and("modifiedDate").as("modifiedDate")
                .and(context -> Document.parse("{ $dayOfMonth: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("day")
                .and(context -> Document.parse("{ $week: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("week")
                .and(context -> Document.parse("{ $month: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("month");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
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
    public SalesDistribution generateSalesDistribution(final String clientId, final LocalDate dateFilter) {

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("total") // this is critical to make $sum work.
                .and("createdDate").as("createdDate")
                .and(context -> Document.parse("{ $week: {date: '$createdDate', timezone: 'Asia/Taipei'} }")).as("week")
                .and(context -> Document.parse("{ $month: {date: '$createdDate', timezone: 'Asia/Taipei'} }")).as("month");

        final LocalDate firstDayOfYear = dateFilter.with(TemporalAdjusters.firstDayOfYear());
        final LocalDate firstDayOfNextYear = dateFilter.with(TemporalAdjusters.firstDayOfNextYear());

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(firstDayOfYear).lt(firstDayOfNextYear));

        final Object[] weeks = IntStream.rangeClosed(0, 53).boxed().toArray(Integer[]::new);

        final BucketOperation salesByWeek = Aggregation.bucket("week").withBoundaries(weeks).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$week")).as("week")
                .andOutput(context -> new Document("$first", "$createdDate")).as("date");

        final BucketOperation salesByMonth = Aggregation.bucket("month").withBoundaries(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$month")).as("month")
                .andOutput(context -> new Document("$first", "$createdDate")).as("date");


        final FacetOperation facets = Aggregation
                .facet(salesByMonth).as("salesByMonth")
                .and(salesByWeek).as("salesByWeek");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<SalesDistribution> result = mongoTemplate.aggregate(aggregations, SalesDistribution.class);
        SalesDistribution salesDistribution = result.getUniqueMappedResult();

        if (salesDistribution != null) {
            enhanceResult(salesDistribution, dateFilter);
        } else {
            salesDistribution = new SalesDistribution();
        }

        return salesDistribution;
    }

    private void enhanceResult(final SalesDistribution salesDistribution, final LocalDate dateFilter) {

        if (salesDistribution.getSalesByMonth().size() != 12) {
            final Map<String, SalesDistribution.MonthlySales> salesMap = salesDistribution.getSalesByMonth().stream()
                    .collect(Collectors.toMap(SalesDistribution.MonthlySales::getMonth, s -> s));
            final ArrayList<SalesDistribution.MonthlySales> enhancedMonthlySales = new ArrayList<>();

            final ValueRange range = dateFilter.range(ChronoField.MONTH_OF_YEAR);

            for (int i = 1; i <= range.getMaximum(); i++) {
                final String key = String.valueOf(i);
                SalesDistribution.MonthlySales monthlySales = salesMap.get(key);

                if (monthlySales == null) {
                    monthlySales = new SalesDistribution.MonthlySales();
                    monthlySales.setId(key);
                    monthlySales.setMonth(key);
                    final LocalDate month = dateFilter.withMonth(i);
                    monthlySales.setDate(month);
                }

                enhancedMonthlySales.add(monthlySales);
            }

            salesDistribution.setSalesByMonth(enhancedMonthlySales);
        }
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }

}
