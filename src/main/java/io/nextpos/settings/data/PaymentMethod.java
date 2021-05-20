package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectOrdering;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity(name = "payment_methods")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "paymentKey"))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PaymentMethod extends BaseObject implements ObjectOrdering<Integer> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String paymentKey;

    private String displayName;

    private Integer ordering;

    public PaymentMethod(String paymentKey, String displayName, int ordering) {
        this.paymentKey = paymentKey;
        this.displayName = displayName;
        this.ordering = ordering;
    }
}
