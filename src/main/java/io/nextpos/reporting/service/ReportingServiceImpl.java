package io.nextpos.reporting.service;

import com.mongodb.BasicDBObject;
import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.ReportingParameter;
import io.nextpos.reporting.data.SalesReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
     *
     * How to solve the "FieldPath field names may not contain '.'" error:
     * https://stackoverflow.com/questions/43694556/fieldpath-field-names-may-not-contain/43694591
     *
     * MongoDB push example:
     * https://stackoverflow.com/questions/39393672/mongodb-aggregate-push-multiple-fields-in-java-spring-data
     *
     * @param client
     * @param reportingParameter
     * @return
     */
    @Override
    public SalesReport generateSalesReport(final Client client, final ReportingParameter reportingParameter) {

        final LocalDateTime startOfDay = LocalDate.now().atTime(8, 0);
        final LocalDateTime endOfDay = startOfDay.plusDays(1);

        final ProjectionOperation projection = Aggregation.project("clientId", "total", "orderLineItems", "modifiedDate");
        final UnwindOperation flattenLineItems = Aggregation.unwind("orderLineItems");

        final MatchOperation clientMatcher = Aggregation.match(
                Criteria.where("clientId").is(client.getId())
                        .and("modifiedDate").gte(startOfDay).lte(endOfDay));

        final ConvertOperators.ToDecimal subTotalToDecimal = createToDecimal("orderLineItems.subTotal.amountWithTax");
        final GroupOperation lineItemsSubTotal = Aggregation.group(
                Fields.fields().and("clientId", "$clientId").and("name", "$orderLineItems.productSnapshot.name"))
                .sum(subTotalToDecimal).as("lineItemsSubTotal")
                .first("modifiedDate").as("salesDate")
                .first("orderLineItems.productSnapshot.name").as("productName");

        final ConvertOperators.ToDecimal totalToDecimal = createToDecimal("lineItemsSubTotal");
        final GroupOperation ordersTotal = Aggregation.group("clientId").sum(totalToDecimal).as("salesTotal")
                .first("salesDate").as("salesDate")
                .push(new BasicDBObject().append("name", "$productName").append("amount", "$lineItemsSubTotal")).as("products");

        final TypedAggregation<Order> salesAmountOfTheDay = Aggregation.newAggregation(Order.class,
                projection,
                flattenLineItems,
                clientMatcher,
                lineItemsSubTotal, ordersTotal);

        final AggregationResults<SalesReport> result = mongoTemplate.aggregate(salesAmountOfTheDay, SalesReport.class);

        return result.getUniqueMappedResult();
    }

    private ConvertOperators.ToDecimal createToDecimal(String fieldReference) {
        return ConvertOperators.valueOf(fieldReference).convertToDecimal();
    }


}
