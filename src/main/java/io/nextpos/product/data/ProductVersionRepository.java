package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, String> {

    @Query("select p from io.nextpos.product.data.ProductVersion p where p.product.client = ?1 and p.version = ?2 order by p.product.ordering, p.productName asc")
    List<ProductVersion> findAllProductsByClient(Client client, Version version);

    @Query("select p from io.nextpos.product.data.ProductVersion p where p.product.client = :client and p.version = :version and (lower(p.productName) like %:keyword% or lower(sku) like %:keyword% or lower(description) like %:keyword%) order by p.productName asc")
    List<ProductVersion> findAllProductsByKeyword(@Param("client") Client client, @Param("version") Version version, @Param("keyword") String keyword);

    List<ProductVersion> findAllByProduct_ClientAndVersionAndProduct_Pinned(Client client, Version version, boolean pinned, Sort sort);

    @Modifying
    @Query("delete from io.nextpos.product.data.ProductVersion p where p.product = ?1 and p.version = io.nextpos.product.data.Version.RETIRED")
    void deleteRetiredProductVersions(Product product);
}
