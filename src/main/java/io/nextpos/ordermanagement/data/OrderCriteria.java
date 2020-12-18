package io.nextpos.ordermanagement.data;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

public class OrderCriteria {

    private String tableName;

    private String membershipId;

    public static OrderCriteria instance() {
        return new OrderCriteria();
    }

    public OrderCriteria tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public OrderCriteria membershipId(String membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public void decorateCriteria(Criteria criteria) {

        if (StringUtils.isNotBlank(tableName)) {
            criteria.and("tables.tableName").is(tableName);
        }

        if (StringUtils.isNotBlank(membershipId)) {
            criteria.and("lookupMembershipId").is(membershipId);
        }
    }
}
