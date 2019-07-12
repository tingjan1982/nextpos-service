package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLabelRepository extends JpaRepository<ProductLabel, String> {

    Optional<ProductLabel> findByNameAndClient(String name, Client client);
}