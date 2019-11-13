package io.nextpos.product.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRelationRepository<T extends ProductOptionRelation> extends JpaRepository<T, Long> {
}
