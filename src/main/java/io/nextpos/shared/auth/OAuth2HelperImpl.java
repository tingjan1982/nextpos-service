package io.nextpos.shared.auth;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuth2HelperImpl implements OAuth2Helper {

    private final ClientService clientService;

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public OAuth2HelperImpl(final ClientService clientService, AuthenticationHelper authenticationHelper) {
        this.clientService = clientService;
        this.authenticationHelper = authenticationHelper;
    }

    @Deprecated
    @Override
    public String getCurrentPrincipal() {
        return authenticationHelper.resolveCurrentUsername();
    }

    @Deprecated
    @Override
    public ClientUser resolveCurrentClientUser(Client client) {
        return clientService.getClientUser(client, authenticationHelper.resolveCurrentUsername());
    }
}
