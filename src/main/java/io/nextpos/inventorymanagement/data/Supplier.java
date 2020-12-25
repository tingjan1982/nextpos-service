package io.nextpos.inventorymanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Supplier extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private String name;

    private ContactInfo contactInfo = ContactInfo.builder().build();

    private List<String> tags;

    private Map<String, String> attributes = new HashMap<>();

    public Supplier(String clientId, String name) {
        this.clientId = clientId;
        this.name = name;
    }

    @Data
    @Builder
    public static class ContactInfo {

        private String contactPerson;

        private String contactEmail;

        private String contactNumber;

        private String contactAddress;
    }
}
