package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ClientOwnershipViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ClientObjectOwnershipServiceImplTest {

    @Autowired
    private ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;


    @Test
    void checkOwnership() {

        Client client1 = new Client("client1", "user1@mail.co", "password", "TW");
        Client client2 = new Client("client2", "user2@mail.co", "password", "TW");

        clientService.createClient(client1);
        clientService.createClient(client2);

        assertThat(client1).isEqualTo(clientService.getClient("USER1").orElseThrow());

        final Product product = new Product(client1, DummyObjects.dummyProductVersion());
        productService.saveProduct(product);

        assertDoesNotThrow(() -> clientObjectOwnershipService.checkOwnership(client1, () -> product));
        assertThrows(ClientOwnershipViolationException.class, () -> clientObjectOwnershipService.checkOwnership(client2, () -> product));
    }
}