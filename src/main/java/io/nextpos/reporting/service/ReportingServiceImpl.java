package io.nextpos.reporting.service;

import com.mongodb.BasicDBObject;
import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;
import io.nextpos.reporting.data.ReportDateParameter;
import io.nextpos.reporting.data.SalesReport;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class ReportingServiceImpl implements ReportingService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReportingServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    /**
     * https://stackoverflow.com/questions/50058304/group-by-day-and-item-total-but-output-item-names-as-keys
     * <p>
     * How to solve the "FieldPath field names may not contain '.'" error:
     * https://stackoverflow.com/questions/43694556/fieldpath-field-names-may-not-contain/43694591
     * <p>
     * MongoDB push example:
     * https://stackoverflow.com/questions/39393672/mongodb-aggregate-push-multiple-fields-in-java-spring-data
     *
     * @param client
     * @param reportDateParameter
     * @return
     */
    @Override
    public SalesReport generateSalesReport(final Client client, final ReportDateParameter reportDateParameter) {
        Validate.notNull(client);
        Validate.notNull(reportDateParameter);

        final ProjectionOperation projection = Aggregation.project("clientId", "total", "orderLineItems", "modifiedDate");
        final UnwindOperation flattenLineItems = Aggregation.unwind("orderLineItems");

        final MatchOperation clientMatcher = Aggregation.match(
                Criteria.where("clientId").is(client.getId())
                        .and("modifiedDate").gte(reportDateParameter.getFromDate()).lte(reportDateParameter.getToDate()));

        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("orderLineItems.subTotal.amountWithTax");
        final GroupOperation lineItemsSubTotal = Aggregation.group(
                Fields.fields().and("clientId", "$clientId").and("name", "$orderLineItems.productSnapshot.name"))
                .sum(subTotalToDecimal).as("lineItemsSubTotal")
                .first("orderLineItems.productSnapshot.name").as("productName");

        final ConvertOperators.ToDecimal totalToDecimal = createToDecimal("lineItemsSubTotal");
        final GroupOperation ordersTotal = Aggregation.group("clientId").sum(totalToDecimal).as("salesTotal")
                .push(new BasicDBObject().append("name", "$productName").append("amount", "$lineItemsSubTotal")).as("salesByProducts");

        final TypedAggregation<Order> salesAmountOfTheDay = Aggregation.newAggregation(Order.class,
                projection,
                flattenLineItems,
                clientMatcher,
                lineItemsSubTotal, ordersTotal);

        final AggregationResults<SalesReport> result = mongoTemplate.aggregate(salesAmountOfTheDay, SalesReport.class);
        final SalesReport salesReport = result.getUniqueMappedResult();

        if (salesReport != null) {
            salesReport.setFromDate(reportDateParameter.getFromDate());
            salesReport.setToDate(reportDateParameter.getToDate());
        }

        return salesReport;
    }

    @Override
    public OrderStateAverageTimeReport generateStateTransitionAverageTimeReport(final Client client, final OrderStateParameter orderStateParameter) {


        final ProjectionOperation projection = Aggregation.project("orderId", "clientId", "stateChanges");
        final MatchOperation clientMatcher = Aggregation.match(Criteria.where("clientId").is(client.getId()));
        final UnwindOperation flattenStateChanges = Aggregation.unwind("stateChanges");

        final ReportDateParameter reportDateParameter = orderStateParameter.getDateParameter();
        final MatchOperation stateMatcher = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("stateChanges.timestamp").gte(reportDateParameter.getFromDate()).lte(reportDateParameter.getToDate()),
                        new Criteria().orOperator(
                                Criteria.where("stateChanges.fromState").is(orderStateParameter.getFromState()),
                                Criteria.where("stateChanges.toState").is(orderStateParameter.getToState())))
        );

        final GroupOperation groupByOrder = Aggregation.group("clientId", "orderId").min("stateChanges.timestamp").as("fromStamp").max("stateChanges.timestamp").as("toStamp");
        final ProjectionOperation computeTimeDifference = Aggregation.project("clientId", "orderId").and("toStamp").minus("fromStamp").as("stampDiff");
        final GroupOperation averageWaitTime = Aggregation.group("clientId")
                .avg("stampDiff").as("averageWaitTime");

        final AggregationResults<OrderStateAverageTimeReport> result = mongoTemplate.aggregate(Aggregation.newAggregation(OrderStateChange.class,
                projection,
                clientMatcher,
                flattenStateChanges,
                stateMatcher,
                groupByOrder,
                computeTimeDifference,
                averageWaitTime), OrderStateAverageTimeReport.class);

        final OrderStateAverageTimeReport report = result.getUniqueMappedResult();

        if (report != null) {
            report.setFromState(orderStateParameter.getFromState());
            report.setToState(orderStateParameter.getToState());
        }

        return report;
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }

}
