package io.nextpos.membership.service;

import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.OrderTopRanking;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@ChainedTransaction
public class MembershipReportServiceImpl implements MembershipReportService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public MembershipReportServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Order> getRecentOrders(Membership membership, int orderCount) {

        final Query query = new Query().with(Sort.by(Sort.Order.desc("createdDate"))).limit(orderCount);
        query.addCriteria(where("membership").is(membership));

        return mongoTemplate.find(query, Order.class);
    }

    @Override
    public List<OrderTopRanking> getTopRankingOrderLineItems(Membership membership, int limit) {

        final ProjectionOperation projection = Aggregation.project()
                .and("state").as("state")
                .and("orderLineItems").as("lineItems")
                .and("lookupMembershipId").as("membershipId");

        final MatchOperation filter = Aggregation.match(
                where("membershipId").is(membership.getId())
                        .and("state").ne(Order.OrderState.DELETED));

        final UnwindOperation flattenLineItems = Aggregation.unwind("lineItems");

        final GroupOperation productCount = Aggregation.group("lineItems.productSnapshot.name")
                .first("lineItems.productSnapshot.name").as("productName")
                .sum("lineItems.quantity").as("quantity");

        final SortOperation sortByProductCount = Aggregation.sort(Sort.by(Sort.Order.desc("quantity")));
        final LimitOperation topFive = Aggregation.limit(limit);

        final TypedAggregation<Order> aggregations = Aggregation.newAggregation(Order.class,
                projection,
                filter,
                flattenLineItems,
                productCount,
                sortByProductCount,
                topFive);

        final AggregationResults<OrderTopRanking> results = mongoTemplate.aggregate(aggregations, OrderTopRanking.class);

        return results.getMappedResults();
    }
}
