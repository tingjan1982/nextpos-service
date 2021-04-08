package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.bean.ClientAuthToken;

public interface ClientAuthTokenService {

    String encodeClientAuthToken(Client client, String password);

    ClientAuthToken decodeClientAuthToken(String encodedToken);
}
