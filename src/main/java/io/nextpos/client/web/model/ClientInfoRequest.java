package io.nextpos.client.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientInfoRequest {

    private String ownerName;

    private String contactNumber;

    private String contactAddress;

    private String operationStatus;

    private String leadSource;

    private String requirements;
}
