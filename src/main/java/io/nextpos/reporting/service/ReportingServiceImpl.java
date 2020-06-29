package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * https://stackoverflow.com/questions/50058304/group-by-day-and-item-total-but-output-item-names-as-keys
 * <p>
 * How to solve the "FieldPath field names may not contain '.'" error:
 * https://stackoverflow.com/questions/43694556/fieldpath-field-names-may-not-contain/43694591
 * <p>
 * MongoDB push example:
 * https://stackoverflow.com/questions/39393672/mongodb-aggregate-push-multiple-fields-in-java-spring-data
 */
@Service
@Transactional
public class ReportingServiceImpl implements ReportingService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ReportingServiceImpl(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    @Override
    public OrderStateAverageTimeReport generateStateTransitionAverageTimeReport(final Client client, final OrderStateParameter orderStateParameter) {

        final ProjectionOperation projection = Aggregation.project("orderId", "clientId", "stateChanges");
        final MatchOperation clientMatcher = Aggregation.match(Criteria.where("clientId").is(client.getId()));
        final UnwindOperation flattenStateChanges = Aggregation.unwind("stateChanges");

        final ZonedDateRange zonedDateRange = orderStateParameter.getZonedDateRange();
        final MatchOperation stateMatcher = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("stateChanges.timestamp").gte(zonedDateRange.getFromDate()).lte(zonedDateRange.getToDate()),
                        new Criteria().orOperator(
                                Criteria.where("stateChanges.fromState").is(orderStateParameter.getFromState()),
                                Criteria.where("stateChanges.toState").is(orderStateParameter.getToState())))
        );

        final GroupOperation groupByOrder = Aggregation.group("orderId")
                .first("clientId").as("clientId")
                .min("stateChanges.timestamp").as("fromStamp")
                .max("stateChanges.timestamp").as("toStamp");
        
        final ProjectionOperation computeTimeDifference = Aggregation.project("clientId", "orderId")
                .and("toStamp").minus("fromStamp").as("stampDiff");

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

        OrderStateAverageTimeReport report = result.getUniqueMappedResult();

        if (report == null) {
            report = new OrderStateAverageTimeReport();
            report.setAverageWaitTime(-1);
        }

        report.setFromState(orderStateParameter.getFromState());
        report.setToState(orderStateParameter.getToState());

        return report;
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }

}
