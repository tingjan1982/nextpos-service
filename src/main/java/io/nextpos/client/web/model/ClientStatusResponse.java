package io.nextpos.client.web.model;

import io.nextpos.subscription.data.ClientSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientStatusResponse {

    private String clientId;

    private SubscriptionResponse subscription;

    private boolean noTableLayout;

    private boolean noTable;

    private boolean noCategory;

    private boolean noProduct;

    private boolean noWorkingArea;

    private boolean noPrinter;

    private boolean noElectronicInvoice;

    @Data
    @AllArgsConstructor
    public static class SubscriptionResponse {

        private String planName;

        private ClientSubscription.SubscriptionStatus status;

        private List<String> restrictedFeatures;
    }
}
