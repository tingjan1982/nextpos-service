package io.nextpos.shared.auth;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

public interface OAuth2Helper {

    String getCurrentPrincipal();

    ClientUser resolveCurrentClientUser(Client client);
}
