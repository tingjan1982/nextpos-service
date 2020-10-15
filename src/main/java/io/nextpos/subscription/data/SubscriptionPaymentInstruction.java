package io.nextpos.subscription.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPaymentInstruction extends MongoBaseObject {

    @Id
    private String id;

    private String countryCode;

    private String emailTemplateId;

    public SubscriptionPaymentInstruction(String countryCode, String emailTemplateId) {
        this.countryCode = countryCode;
        this.emailTemplateId = emailTemplateId;
    }
}
