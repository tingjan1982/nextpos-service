package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, String> {

    @Query("select p from io.nextpos.product.data.ProductVersion p where p.product.client = ?1 and p.version = ?2 order by p.productName asc")
    List<ProductVersion> findAllProductsByClient(Client client, Version version);

    List<ProductVersion> findAllByProduct_ClientAndVersionAndProduct_Pinned(Client client, Version version, boolean pinned, Sort sort);

    @Modifying
    @Query("delete from io.nextpos.product.data.ProductVersion p where p.product = ?1 and p.version = io.nextpos.product.data.Version.RETIRED")
    void deleteRetiredProductVersions(Product product);
}
