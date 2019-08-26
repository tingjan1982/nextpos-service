package io.nextpos.tablelayout.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableLayoutRepository extends JpaRepository<TableLayout, String> {

    List<TableLayout> findAllByClient(Client client);
}
