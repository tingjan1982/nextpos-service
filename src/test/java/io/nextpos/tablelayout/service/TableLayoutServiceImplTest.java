package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.tablelayout.data.TableLayout;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TableLayoutServiceImplTest {

    @Autowired
    private TableLayoutService tableLayoutService;

    @Autowired
    private ClientRepository clientRepository;


    @Test
    void crudTableLayout() {

        final Client client = DummyObjects.dummyClient();
        clientRepository.save(client);

        final TableLayout firstFloor = new TableLayout(client, "first floor", 10, 10);
        final TableLayout.TableDetails tableA = new TableLayout.TableDetails("table A", 1, 3);
        tableA.setCapacity(4);

        final TableLayout.TableDetails tableB = new TableLayout.TableDetails("table B", 0, 1);
        tableB.setCapacity(6);

        firstFloor.addTableDetails(tableA).addTableDetails(tableB);

        final TableLayout savedLayout = tableLayoutService.saveTableLayout(firstFloor);

        final TableLayout tableLayoutToCheck = tableLayoutService.getTableLayout(savedLayout.getId());

        assertThat(tableLayoutToCheck.getId()).isNotNull();
        assertThat(tableLayoutToCheck.getTotalCapacity()).isEqualTo(10);
        assertThat(tableLayoutToCheck.getTables()).hasSize(2);
        assertThat(tableLayoutToCheck.getTables()).satisfies(t -> {
            assertThat(t.getId()).isEqualTo(tableLayoutToCheck.getId() + "-1-3"); // table details id is defined in TableDetailsIdGenerator.
            assertThat(t.getXCoordinate()).isEqualTo(1);
            assertThat(t.getYCoordinate()).isEqualTo(3);
        }, Index.atIndex(0));

        assertThat(tableLayoutService.getTableLayouts(client)).hasSize(1);
    }

    @Test
    void addTableOutOfGridSize() {

        final Client client = DummyObjects.dummyClient();
        final TableLayout firstFloor = new TableLayout(client, "first floor", 5, 5);


        assertThatThrownBy(() -> firstFloor.addTableDetails(new TableLayout.TableDetails("table A", -1, 0)))
                .isInstanceOf(GeneralApplicationException.class);

        assertThatThrownBy(() -> firstFloor.addTableDetails(new TableLayout.TableDetails("table A", 5, 0)))
                .isInstanceOf(GeneralApplicationException.class);

        assertThatThrownBy(() -> firstFloor.addTableDetails(new TableLayout.TableDetails("table A", 0, -1)))
                .isInstanceOf(GeneralApplicationException.class);

        assertThatThrownBy(() -> firstFloor.addTableDetails(new TableLayout.TableDetails("table A", 0, 5)))
                .isInstanceOf(GeneralApplicationException.class);
    }
}