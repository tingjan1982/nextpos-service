package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.OrderIdCounter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class ClientsResponse {

    private List<ClientDetailsResponse> results;

    @Data
    public static class ClientDetailsResponse {

        private final String id;

        private final String clientName;

        private final String username;

        private final String country;

        private final String timezone;

        private final Client.Status status;

        private final Date createdDate;

        private final String lastOrderDate;

        private final int orderCount;

        public ClientDetailsResponse(Client client, OrderIdCounter.OrderCounterSummary orderCounterSummary) {

            id = client.getId();
            clientName = client.getClientName();
            username = client.getUsername();
            country = client.getCountryCode();
            timezone = client.getTimezone();
            status = client.getStatus();
            createdDate = client.getCreatedTime();

            lastOrderDate = orderCounterSummary.getOrderIdPrefix();
            orderCount = orderCounterSummary.getOrderCount();
        }
    }
}
