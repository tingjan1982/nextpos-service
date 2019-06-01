package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;

@Entity(name = "client_order")
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private OrderState state;

    private BigDecimal orderTotalWithTax;

    private BigDecimal orderTax;


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
