package io.nextpos.workingarea.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrinterRepository extends JpaRepository<Printer, String> {

    List<Printer> findAllByClient(Client client);
}
