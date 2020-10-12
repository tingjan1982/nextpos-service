package io.nextpos.product.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOptionRelationRepository<T extends ProductOptionRelation> extends JpaRepository<T, Long> {

    Long countByProductOption(ProductOption productOption);

    List<? extends ProductOptionRelation> findAllByProductOption(ProductOption productOption);
}
