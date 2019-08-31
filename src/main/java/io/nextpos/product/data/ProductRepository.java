package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllByClientAndProductLabel(Client client, ProductLabel productLabel);
}
