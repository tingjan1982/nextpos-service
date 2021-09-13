package io.nextpos.reporting.service;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.ReportEnhancer;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;
import io.nextpos.shared.service.annotation.MongoTransaction;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@MongoTransaction
public class SalesReportServiceImpl implements SalesReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public SalesReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public RangedSalesReport generateRangedSalesReport(final String clientId, final ZonedDateRange zonedDateRange) {

        final LookupOperation lookupTransactions = Aggregation.lookup("orderTransaction", "lookupOrderId", "orderId", "transactions");
        final UnwindOperation flattenTransactions = Aggregation.unwind("transactions");

        ProjectionOperation projection = createProjection(zonedDateRange);
        final MatchOperation filter = createMatchFilter(clientId, zonedDateRange);

        final GroupOperation salesTotal = Aggregation.group("clientId")
                .sum("orderTotal").as("salesTotal")
                .sum("serviceCharge").as("serviceChargeTotal")
                .sum("discount").as("discountTotal");

        final GroupOperation salesByPaymentMethod = Aggregation.group(Fields.from(Fields.field("status", "transactions.status"), Fields.field("paymentMethod", "transactions.paymentDetails.paymentMethod")))
                .first("transactions.status").as("status")
                .first("transactions.paymentDetails.paymentMethod").as("paymentMethod")
                .sum(createToDecimal("transactions.orderTotal")).as("orderTotal")
                .sum(createToDecimal("transactions.settleAmount")).as("settleAmount")
                .sum("serviceCharge").as("serviceCharge")
                .sum("discount").as("discount");

        final UnwindOperation flattenLineItems = Aggregation.unwind("lineItems");

        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("lineItems.lineItemSubTotal");
        final GroupOperation salesByProduct = Aggregation.group("lineItems.productSnapshot.name")
                .sum(subTotalToDecimal).as("productSales")
                .sum("lineItems.quantity").as("salesQuantity")
                .first("lineItems.productSnapshot.name").as("productName");
        final SortOperation sortSalesByProduct = Aggregation.sort(Sort.Direction.DESC, "salesQuantity");

        final GroupOperation salesByLabel = Aggregation.group("lineItems.productSnapshot.labelId")
                .sum(subTotalToDecimal).as("productSales")
                .sum("lineItems.quantity").as("salesQuantity")
                .first("lineItems.productSnapshot.label").as("productLabel");

        final FacetOperation facets = Aggregation
                .facet(salesTotal).as("totalSales")
                .and(flattenTransactions, salesByPaymentMethod).as("salesByPaymentMethod")
                .and(createSalesByRangeFacet()).as("salesByRange")
                .and(flattenLineItems, salesByProduct, sortSalesByProduct).as("salesByProduct")
                .and(flattenLineItems, salesByLabel, sortSalesByProduct).as("salesByLabel");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                lookupTransactions,
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

    @Override
    public RangedSalesReport generateSalesRankingReport(final String clientId,
                                                        final ZonedDateRange zonedDateRange,
                                                        final String labelId) {

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("orderTotal")
                .and("orderLineItems").as("lineItems")
                .and("createdDate").as("createdDate");

        final UnwindOperation flattenLineItems = Aggregation.unwind("lineItems");

        final MatchOperation filter = createMatchFilter(clientId, zonedDateRange);

        final GroupOperation salesTotal = Aggregation.group("clientId")
                .sum("orderTotal").as("salesTotal");

        final MatchOperation labelFilter = Aggregation.match(Criteria.where("lineItems.productSnapshot.labelId").is(labelId));
        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("lineItems.lineItemSubTotal");
        final GroupOperation salesByProduct = Aggregation.group("lineItems.productSnapshot.name")
                .sum(subTotalToDecimal).as("productSales")
                .sum("lineItems.quantity").as("salesQuantity")
                .first("lineItems.productSnapshot.name").as("productName");
        final SortOperation sortSalesByProduct = Aggregation.sort(Sort.Direction.DESC, "productSales");

        final FacetOperation facets = Aggregation
                .facet(salesTotal).as("totalSales")
                .and(flattenLineItems, labelFilter, salesByProduct, sortSalesByProduct).as("salesByProduct");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                facets);

        final AggregationResults<RangedSalesReport> result = mongoTemplate.aggregate(aggregations, RangedSalesReport.class);
        final RangedSalesReport results = result.getUniqueMappedResult();

        if (results != null) {
            if (results.hasResult()) {
                final int productsTotalQuantity = results.getSalesByProduct().stream()
                        .mapToInt(RangedSalesReport.SalesByProduct::getSalesQuantity)
                        .sum();

                results.getSalesByProduct().forEach(s -> {
                    final BigDecimal percentage = new BigDecimal((double) s.getSalesQuantity() / productsTotalQuantity * 100).setScale(2, RoundingMode.UP);
                    s.setPercentage(percentage);
                });
            }
        }

        return results;
    }

    private ProjectionOperation createProjection(final ZonedDateRange zonedDateRange) {

        final String timezone = zonedDateRange.getClientTimeZone().getId();

        return Aggregation.project("id", "clientId", "state", "createdDate", "transactions")
                .and(createToDecimal("orderTotal")).as("orderTotal")
                .and(createToDecimal("serviceCharge")).as("serviceCharge")
                .and(createToDecimal("discount")).as("discount")
                .and("orderLineItems").as("lineItems")
                .and(context -> Document.parse("{ $dateToString: {format: '%Y-%m-%d', date: '$createdDate', timezone: '" + timezone + "'} }")).as("date")
                .and(context -> Document.parse("{ $dayOfYear: {date: '$createdDate', timezone: '" + timezone + "'} }")).as("dayOfYear");
    }

    private MatchOperation createMatchFilter(String clientId,
                                             ZonedDateRange zonedDateRange) {

        return Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").in(Order.OrderState.SETTLED, Order.OrderState.COMPLETED)
                        .and("createdDate").gte(zonedDateRange.getFromDate()).lte(zonedDateRange.getToDate()));
    }

    private GroupOperation createSalesByRangeFacet() {

        return Aggregation.group("date")
                .last("dayOfYear").as("dayOfYear")
                .sum("orderTotal").as("total")
                .last("date").as("date")
                .count().as("orderCount");
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
            final int productsTotalQuantity = results.getSalesByProduct().stream()
                    .mapToInt(RangedSalesReport.SalesByProduct::getSalesQuantity)
                    .sum();

            results.getSalesByProduct().forEach(s -> {
                final BigDecimal percentage = new BigDecimal((double) s.getSalesQuantity() / productsTotalQuantity * 100).setScale(2, RoundingMode.UP);
                s.setPercentage(percentage);
            });

            final int labelsTotalQuantity = results.getSalesByLabel().stream()
                    .mapToInt(RangedSalesReport.SalesByLabel::getSalesQuantity)
                    .sum();

            results.getSalesByLabel().forEach(s -> {
                final BigDecimal percentage = new BigDecimal((double) s.getSalesQuantity() / labelsTotalQuantity * 100).setScale(2, RoundingMode.UP);
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
                .and("createdDate").as("createdDate")
                .and(context -> Document.parse("{ $dayOfMonth: {date: '$createdDate', timezone: 'Asia/Taipei'} }")).as("day")
                .and(context -> Document.parse("{ $week: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("week")
                .and(context -> Document.parse("{ $month: {date: '$modifiedDate', timezone: 'Asia/Taipei'} }")).as("month");

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(LocalDate.now().withDayOfMonth(1)).lt(LocalDate.now().plusMonths(1).withDayOfMonth(1)));

        final BucketOperation dailySales = Aggregation.bucket("day").withBoundaries(today, today + 1).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$day")).as("dayOfMonth");

        final BucketOperation weeklySales = Aggregation.bucket("week").withBoundaries(currentWeek, currentWeek + 1).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$week")).as("week");

        final GroupOperation monthSales = Aggregation.group("month")
                .sum("total").as("total")
                .first("month").as("month");

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
    public SalesDistribution generateSalesDistribution(final String clientId, final ZoneId zoneId, final LocalDate dateFilter) {

        final String timezone = zoneId.getId();

        final ProjectionOperation projection = Aggregation.project("clientId")
                .and("state").as("state")
                .and(createToDecimal("orderTotal")).as("total") // this is critical to make $sum work.
                .and("createdDate").as("createdDate")
                .and(context -> Document.parse("{ $dateToString: {format: '%Y-%m-%d', date: '$createdDate', timezone: '" + timezone + "'} }")).as("date")
                .and(context -> Document.parse("{ $week: {date: '$createdDate', timezone: '" + timezone + "'} }")).as("week")
                .and(context -> Document.parse("{ $month: {date: '$createdDate', timezone: '" + timezone + "'} }")).as("month");

        final LocalDate firstDayOfYear = dateFilter.with(TemporalAdjusters.firstDayOfYear());
        final LocalDateTime lastDayOfYear = dateFilter.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59);

        final MatchOperation filter = Aggregation.match(
                Criteria.where("clientId").is(clientId)
                        .and("state").ne(Order.OrderState.DELETED)
                        .and("createdDate").gte(firstDayOfYear).lt(lastDayOfYear));

        final Object[] weeks = IntStream.rangeClosed(0, 53).boxed().toArray(Integer[]::new);

        final BucketOperation salesByWeek = Aggregation.bucket("week").withBoundaries(weeks).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$week")).as("week")
                .andOutput("date").last().as("date");

        final BucketOperation salesByMonth = Aggregation.bucket("month").withBoundaries(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13).withDefaultBucket("Other")
                .andOutput(AccumulatorOperators.Sum.sumOf("total")).as("total")
                .andOutput(context -> new Document("$first", "$month")).as("month")
                .andOutput("date").last().as("date");

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
