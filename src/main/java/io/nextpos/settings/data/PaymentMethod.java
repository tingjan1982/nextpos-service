package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectOrdering;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "payment_methods")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "paymentKey"))
@Getter
@Setter
@ToString
@RequiredArgsConstructor
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return id.equals(that.id) && paymentKey.equals(that.paymentKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, paymentKey);
    }
}
