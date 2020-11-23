package io.nextpos.linkedaccount.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.linkedaccount.data.LinkedClientAccount;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class LinkedClientAccountServiceImplTest {

    private final LinkedClientAccountService linkedClientAccountService;

    private final ClientService clientService;

    private final AuthenticationManager authenticationManager;

    @Autowired
    LinkedClientAccountServiceImplTest(LinkedClientAccountService linkedClientAccountService, ClientService clientService, AuthenticationManager authenticationManager) {
        this.linkedClientAccountService = linkedClientAccountService;
        this.clientService = clientService;
        this.authenticationManager = authenticationManager;
    }

    @Test
    void crudLinkedClientAccount() {

        final Client ron = new Client("Ron", "ron@roncafe.bar", "1234", "TW", "Asia/Taipei");
        clientService.saveClient(ron);

        final Client ronXinyi = new Client("Ron Xinyi", "ronxinyi@roncafe.bar", "1234", "TW", "Asia/Taipei");
        clientService.saveClient(ronXinyi);

        final LinkedClientAccount linkedClientAccount = linkedClientAccountService.createLinkedClientAccount(ron, ronXinyi);

        assertThat(linkedClientAccount.getId()).isNotNull();
        assertThat(linkedClientAccount.getSourceClient()).isEqualTo(ron);
        assertThat(linkedClientAccount.getLinkedClients()).contains(ronXinyi);

        final Client attic = new Client("Attic", "attic@roncafe.bar", "1234", "TW", "Asia/Taipei");
        clientService.saveClient(attic);

        linkedClientAccountService.addLinkedClient(linkedClientAccount, ronXinyi);
        linkedClientAccountService.addLinkedClient(linkedClientAccount, attic);

        assertThat(linkedClientAccount.getLinkedClients()).hasSize(2);

        linkedClientAccountService.removeLinkedClient(linkedClientAccount, attic);

        assertThat(linkedClientAccount.getLinkedClients()).hasSize(1);

        LinkedClientAccount retrievedLinkedClientAccount = linkedClientAccountService.getLinkedClientAccount(ron).orElseThrow();
        assertThat(linkedClientAccountService.getLinkedClientAccountByLinkedClient(ronXinyi)).isNotEmpty();
        
        assertThat(retrievedLinkedClientAccount).isEqualTo(linkedClientAccount);

        linkedClientAccountService.deleteLinkedClientAccount(retrievedLinkedClientAccount);

        assertThat(linkedClientAccountService.getLinkedClientAccount(ron)).isEmpty();
    }

    @Test
    @WithMockUser("ron@roncafe.bar")
    void test() {

        final Client ron = new Client("Ron", "ron@roncafe.bar", "1234", "TW", "Asia/Taipei");
        clientService.createClient(ron);

        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(ron.getUsername(), "1234"));
        System.out.println(authentication);
    }
}