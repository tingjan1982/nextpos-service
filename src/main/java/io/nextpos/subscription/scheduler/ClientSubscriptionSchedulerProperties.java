package io.nextpos.subscription.scheduler;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client-subscription.scheduler")
@Data
public class ClientSubscriptionSchedulerProperties {

    private String renewActiveClientSubscriptionsCron;

    private String unpaidSubscriptionInvoicesCron;

    private String lapseActiveClientSubscriptionsCron;
}
