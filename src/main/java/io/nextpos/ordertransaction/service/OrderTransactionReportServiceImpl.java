package io.nextpos.ordertransaction.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
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

        final GroupOperation totalByPaymentMethod = Aggregation.group("transactions.paymentMethodDetails.paymentMethod")
                .first("transactions.paymentMethodDetails.paymentMethod").as("paymentMethod")
                .sum("orderTotal").as("orderTotal")
                .sum("serviceCharge").as("serviceCharge")
                .sum("discount").as("discount")
                .count().as("orderCount");

        final CountOperation totalOrderCount = Aggregation.count().as("orderCount");
        final GroupOperation orderCountByState = Aggregation.group("state").count().as("orderCount");

        final FacetOperation facets = Aggregation.facet(flattenTransactions, totalByPaymentMethod).as("totalByPaymentMethod")
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
