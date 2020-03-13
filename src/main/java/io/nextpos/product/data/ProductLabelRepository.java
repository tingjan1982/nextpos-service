package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLabelRepository extends JpaRepository<ProductLabel, String> {

    Optional<ProductLabel> findByNameAndClient(String name, Client client);

    List<ProductLabel> findAllByClientAndParentLabelIsNull(Client client, Sort sort);
}
