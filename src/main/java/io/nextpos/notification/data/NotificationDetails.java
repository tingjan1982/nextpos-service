package io.nextpos.notification.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class NotificationDetails extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private DeliveryStatus deliveryStatus;

    public NotificationDetails(final String clientId) {
        this.clientId = clientId;
        this.deliveryStatus = DeliveryStatus.PENDING;
    }

    public enum DeliveryStatus {
        PENDING, SUCCESS, FAIL
    }
}
