package io.nextpos.membership.service;

import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.OrderTopRanking;
import io.nextpos.ordermanagement.data.Order;

import java.util.List;

public interface MembershipReportService {

    List<Order> getRecentOrders(Membership membership, int orderCount);

    List<OrderTopRanking> getTopRankingOrderLineItems(Membership membership, int limit);
}
