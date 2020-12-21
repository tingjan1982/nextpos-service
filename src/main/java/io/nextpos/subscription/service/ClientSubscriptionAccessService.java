package io.nextpos.subscription.service;

import io.nextpos.subscription.data.ClientSubscriptionAccess;

public interface ClientSubscriptionAccessService {

    ClientSubscriptionAccess getClientSubscriptionAccess(String clientId);
}
