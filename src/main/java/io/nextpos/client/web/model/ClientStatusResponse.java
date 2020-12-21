package io.nextpos.client.web.model;

import io.nextpos.subscription.data.ClientSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientStatusResponse {

    private SubscriptionResponse subscription;

    private boolean accountNotVerified;

    private boolean noTableLayout;

    private boolean noTable;

    private boolean noCategory;

    private boolean noProduct;

    private boolean noWorkingArea;

    private boolean noPrinter;

    private boolean noElectronicInvoice;

    /**
     * Use /clientSubscriptions/current to get the same information instead.
     */
    @Deprecated
    @Data
    @AllArgsConstructor
    public static class SubscriptionResponse {

        private String planName;

        private ClientSubscription.SubscriptionStatus status;
    }
}
