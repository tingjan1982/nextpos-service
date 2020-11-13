package io.nextpos.client.web.model;

import io.nextpos.client.data.ClientInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientInfoResponse {

    private String ownerName;

    private String contactNumber;

    private String contactAddress;

    private String operationStatus;

    private String leadSource;

    private String requirements;

    public ClientInfoResponse(ClientInfo clientInfo) {
        this.ownerName = clientInfo.getOwnerName();
        this.contactNumber = clientInfo.getContactNumber();
        this.contactAddress = clientInfo.getContactAddress();
        this.operationStatus = clientInfo.getOperationStatus();
        this.leadSource = clientInfo.getLeadSource();
        this.requirements = clientInfo.getRequirements();
    }
}
