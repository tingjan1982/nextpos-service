package io.nextpos.membership.web.model;

import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.OrderTopRanking;
import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class MembershipResponse {

    private String id;

    private String name;

    private String phoneNumber;

    private LocalDate birthday;

    private Membership.Gender gender;

    private List<String> tags;

    private List<RecentOrderResponse> recentOrders;

    private List<OrderTopRanking> topRankings;

    public MembershipResponse(Membership membership) {
        id = membership.getId();
        name = membership.getName();
        phoneNumber = membership.getPhoneNumber();
        birthday = membership.getBirthday();
        gender = membership.getGender();
        tags = membership.getTags();
    }

    @Data
    public static class RecentOrderResponse {

        private String orderId;

        private Date orderDate;

        private BigDecimal orderTotal;

        public RecentOrderResponse(Order order) {
            orderId = order.getId();
            orderDate = order.getCreatedDate();
            orderTotal = order.getOrderTotal();
        }
    }
}
