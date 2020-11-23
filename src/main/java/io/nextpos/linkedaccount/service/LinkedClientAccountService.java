package io.nextpos.linkedaccount.service;

import io.nextpos.client.data.Client;
import io.nextpos.linkedaccount.data.LinkedClientAccount;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface LinkedClientAccountService {

    LinkedClientAccount createLinkedClientAccount(Client sourceClient, Client linkedClient);

    LinkedClientAccount addLinkedClient(LinkedClientAccount linkedClientAccount, Client linkedClient);

    LinkedClientAccount removeLinkedClient(LinkedClientAccount linkedClientAccount, Client linkedClientToRemove);

    Optional<LinkedClientAccount> getLinkedClientAccount(Client sourceClient);

    LinkedClientAccount getLinkedClientAccountOrThrows(Client sourceClient);

    Optional<LinkedClientAccount> getLinkedClientAccountByLinkedClient(Client linkedClient);

    void deleteLinkedClientAccount(LinkedClientAccount linkedClientAccount);

    <R> R getLinkedClientAccountResources(Client client, Function<List<Client>, R> resourceProvider);
}
