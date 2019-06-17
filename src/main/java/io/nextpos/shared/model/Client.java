package io.nextpos.shared.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity(name = "client")
@Data
@EqualsAndHashCode(callSuper = true)
//@NoArgsConstructor
//@AllArgsConstructor
public class Client extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String clientName;

    @Builder(toBuilder = true)
    public Client(final Date createdTime, final Date updatedTime, final String clientName) {
        super(createdTime, updatedTime);
        this.clientName = clientName;
    }
}
