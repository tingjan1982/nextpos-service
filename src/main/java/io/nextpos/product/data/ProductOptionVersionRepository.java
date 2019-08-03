package io.nextpos.product.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductOptionVersionRepository extends JpaRepository<ProductOptionVersion, String> {

    @Modifying
    @Query("delete from io.nextpos.product.data.ProductOptionVersion p where p.productOption = ?1 and p.version = io.nextpos.product.data.Version.RETIRED")
    void deleteRetiredProductOptionVersions(ProductOption productOption);
}
