package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BusinessObjectState;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, String> {

    @Query("select p from io.nextpos.product.data.ProductVersion p where p.product.client = ?1 and p.state = ?2")
    List<ProductVersion> findAllProductsByClient(Client client, BusinessObjectState state, Sort sort);
}
