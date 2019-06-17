package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.BaseObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

@Entity(name = "client_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private OrderState state;

    private BigDecimal orderTotalWithTax;

    private BigDecimal orderTax;

    @Builder(toBuilder = true)
    public Order(final Date createdTime, final Date updatedTime, final OrderState state, final BigDecimal orderTotalWithTax, final BigDecimal orderTax) {
        super(createdTime, updatedTime);
        this.state = state;
        this.orderTotalWithTax = orderTotalWithTax;
        this.orderTax = orderTax;
    }

    public enum OrderState {

        NEW,
        OPEN,
        PARTIALLY_DELIVERED,
        DELIVERED,
        SETTLED,
        CANCELLED,
        REFUNDED
    }
}
