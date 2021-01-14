package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.roles.data.UserRole;

import java.util.Map;

public interface ClientBootstrapService {

    void bootstrapClient(Client client);

    Map<String, UserRole> bootstrapUserRoles(Client client);
}
