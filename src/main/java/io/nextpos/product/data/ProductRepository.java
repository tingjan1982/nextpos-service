package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.workingarea.data.WorkingArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllByClientAndProductLabel(Client client, ProductLabel productLabel);

    boolean existsAllByWorkingArea(WorkingArea workingArea);
    
    boolean existsAllByProductLabel(ProductLabel productLabel);
}
