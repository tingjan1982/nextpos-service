package io.nextpos.linkedaccount.service;

import io.nextpos.client.data.Client;
import io.nextpos.linkedaccount.data.LinkedClientAccount;
import io.nextpos.linkedaccount.data.LinkedClientAccountRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@ChainedTransaction
public class LinkedClientAccountServiceImpl implements LinkedClientAccountService {

    private final LinkedClientAccountRepository linkedClientAccountRepository;

    @Autowired
    public LinkedClientAccountServiceImpl(LinkedClientAccountRepository linkedClientAccountRepository) {
        this.linkedClientAccountRepository = linkedClientAccountRepository;
    }

    @Override
    public LinkedClientAccount createLinkedClientAccount(Client sourceClient, Client linkedClient) {

        final LinkedClientAccount linkedClientAccount = new LinkedClientAccount(sourceClient);
        linkedClientAccount.addLinkedClient(linkedClient);

        return linkedClientAccountRepository.save(linkedClientAccount);
    }

    @Override
    public LinkedClientAccount addLinkedClient(LinkedClientAccount linkedClientAccount, Client linkedClient) {

        linkedClientAccount.addLinkedClient(linkedClient);

        return linkedClientAccountRepository.save(linkedClientAccount);
    }

    @Override
    public LinkedClientAccount removeLinkedClient(LinkedClientAccount linkedClientAccount, Client linkedClientToRemove) {

        linkedClientAccount.removeLinkedClient(linkedClientToRemove);

        return linkedClientAccountRepository.save(linkedClientAccount);
    }

    @Override
    public Optional<LinkedClientAccount> getLinkedClientAccount(Client sourceClient) {
        return linkedClientAccountRepository.findBySourceClient(sourceClient);
    }

    @Override
    public LinkedClientAccount getLinkedClientAccountOrThrows(Client sourceClient) {
        return this.getLinkedClientAccount(sourceClient).orElseThrow(() -> {
            throw new ObjectNotFoundException(sourceClient.getId(), LinkedClientAccount.class);
        });
    }

    @Override
    public Optional<LinkedClientAccount> getLinkedClientAccountByLinkedClient(Client linkedClient) {
        return linkedClientAccountRepository.findByLinkedClients(linkedClient);
    }

    @Override
    public void deleteLinkedClientAccount(LinkedClientAccount linkedClientAccount) {
        linkedClientAccountRepository.delete(linkedClientAccount);
    }

    @Override
    public <R> R getLinkedClientAccountResources(Client client, Function<List<Client>, R> resourceProvider) {

        List<Client> clients = new ArrayList<>();
        clients.add(client);

        this.getLinkedClientAccountByLinkedClient(client).ifPresent(lca -> clients.add(lca.getSourceClient()));

        return resourceProvider.apply(clients);
    }
}
