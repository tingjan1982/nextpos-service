package io.nextpos.ordertransaction.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@MongoTransaction
public class OrderTransactionReportServiceImpl implements OrderTransactionReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public OrderTransactionReportServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ClosingShiftTransactionReport getClosingShiftTransactionReport(final Shift shift) {

        final LookupOperation lookup = Aggregation.lookup("orderTransaction", "lookupOrderId", "orderId", "transactions");

        final ProjectionOperation projection = Aggregation.project("id", "clientId", "state", "createdDate", "transactions")
                .and(createToDecimal("orderTotal")).as("orderTotal")
                .and(createToDecimal("serviceCharge")).as("serviceCharge")
                .and(createToDecimal("discount")).as("discount");

        final UnwindOperation flattenTransactions = Aggregation.unwind("transactions", false);

        final MatchOperation filter = Aggregation.match(Criteria.where("clientId").is(shift.getClientId())
                .and("createdDate").gte(shift.getStart().getTimestamp()).lt(shift.getEnd().getTimestamp()));

        // todo: refactor by considering void order transaction scenarios to reflect correct amount.
        final MatchOperation stateFilter = Aggregation.match(Criteria.where("state").ne(Order.OrderState.DELETED));

        final GroupOperation orderSummary = Aggregation.group("clientId")
                .sum("orderTotal").as("orderTotal")
                .sum("serviceCharge").as("serviceCharge")
                .sum("discount").as("discount")
                .count().as("orderCount");

        final GroupOperation totalByPaymentMethod = Aggregation.group("transactions.paymentDetails.paymentMethod")
                .first("transactions.paymentDetails.paymentMethod").as("paymentMethod")
                .sum(createToDecimal("transactions.orderTotal")).as("orderTotal")
                .sum(createToDecimal("transactions.settleAmount")).as("settleAmount")
                .sum("serviceCharge").as("serviceCharge")
                .sum("discount").as("discount")
                .count().as("orderCount");

        final CountOperation totalOrderCount = Aggregation.count().as("orderCount");
        final GroupOperation orderCountByState = Aggregation.group("state")
                .first("state").as("orderState")
                .count().as("orderCount");

        final ProjectionOperation entries = Aggregation.project()
                .and("transactions.id").as("txId")
                .and("transactions.orderId").as("orderId")
                .and("transactions.orderTotal").as("txOrderTotal")
                .and("transactions.settleAmount").as("txSettleAmount")
                .and("transactions.paymentDetails.paymentMethod").as("paymentMethod")
                .and("orderTotal").as("orderTotal")
                .and("state").as("state");

        final FacetOperation facets = Aggregation.facet(stateFilter, flattenTransactions, totalByPaymentMethod).as("totalByPaymentMethod")
                .and(stateFilter, orderSummary).as("orderSummary")
                //.and(flattenTransactions, entries).as("entries")
                .and(totalOrderCount).as("totalOrderCount")
                .and(orderCountByState).as("orderCountByState");

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                lookup,
                projection,
                filter,
                facets);

        final AggregationResults<ClosingShiftTransactionReport> result = mongoTemplate.aggregate(aggregations, ClosingShiftTransactionReport.class);

        return result.getUniqueMappedResult();
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }
}
