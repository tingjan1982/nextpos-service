package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductOptionVersionRepository extends JpaRepository<ProductOptionVersion, String> {

    @Modifying
    @Query("delete from io.nextpos.product.data.ProductOptionVersion p where p.productOption = ?1 and p.version = io.nextpos.product.data.Version.RETIRED")
    void deleteRetiredProductOptionVersions(ProductOption productOption);

    List<ProductOptionVersion> findAllByProductOptionClientAndVersionOrderByOptionName(Client client, Version version);

    /**
     * https://stackoverflow.com/questions/27543771/best-way-of-handling-entities-inheritance-in-spring-data-jpa
     */
    @Query(value = "select p from io.nextpos.product.data.ProductOptionVersion p where p.productOption.client = ?1 and p.version = ?2 and exists (" +
            "select pol from io.nextpos.product.data.ProductOptionRelation$ProductOptionOfLabel pol where pol.productLabel = ?3 and pol.productOption = p.productOption)")
    List<ProductOptionVersion> findByProductLabel(Client client, Version version, ProductLabel productLabel);
}
