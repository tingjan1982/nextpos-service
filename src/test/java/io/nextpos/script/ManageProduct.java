package io.nextpos.script;

import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.service.DeleteClientService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductRepository;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageProduct {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageProduct.class);

    private final ClientService clientService;

    private final ProductLabelService productLabelService;

    private final ProductService productService;

    private final ProductRepository productRepository;

    private final DeleteClientService deleteClientService;

    private final ClientRepository clientRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ManageProduct(ClientService clientService, ProductLabelService productLabelService, ProductService productService, ProductRepository productRepository, DeleteClientService deleteClientService, ClientRepository clientRepository, MongoTemplate mongoTemplate) {
        this.clientService = clientService;
        this.productLabelService = productLabelService;
        this.productService = productService;
        this.productRepository = productRepository;
        this.deleteClientService = deleteClientService;
        this.clientRepository = clientRepository;
        this.mongoTemplate = mongoTemplate;
    }


    @Test
    void deleteProducts() {

        clientService.getClientByUsername("ron@gmail.com").ifPresent(c -> {

            //常點調酒、舊版調酒、舊版shot
            productLabelService.getProductLabelByName("舊版shot", c).ifPresent(label -> {

                List<Product> products = productRepository.findAllByClientAndProductLabel(c, label);
                System.out.printf("Deleting products (%s)\n", products.size());

                products.forEach(p -> {
                    System.out.printf("Deleting %s - ", p.getDesignVersion().getProductName());
                    productService.deleteProduct(p);
                    System.out.printf("Deleted\n");
                });

                productLabelService.deleteProductLabel(label);

                System.out.println("Deleted label");
            });
        });
    }
}
