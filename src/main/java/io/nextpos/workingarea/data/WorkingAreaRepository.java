package io.nextpos.workingarea.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingAreaRepository extends JpaRepository<WorkingArea, String> {

    List<WorkingArea> findAllByClient(Client client);

    List<WorkingArea> findAllByClientAndVisibility(Client client, WorkingArea.Visibility visibility);
}
