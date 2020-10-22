package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.tablelayout.data.TableLayout;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TableLayoutServiceImplTest {

    @Autowired
    private TableLayoutService tableLayoutService;

    @Autowired
    private Client client;

    @Test
    void crudTableLayout() {

        final TableLayout firstFloor = new TableLayout(client, "first floor");
        final TableLayout savedLayout = tableLayoutService.saveTableLayout(firstFloor);

        final TableLayout.TableDetails tableA = new TableLayout.TableDetails("table A", 4);
        final TableLayout.TableDetails tableB = new TableLayout.TableDetails("table B", 6);

        savedLayout.addTableDetails(tableA).addTableDetails(tableB);
        tableLayoutService.saveTableLayout(savedLayout);

        final TableLayout tableLayoutToCheck = tableLayoutService.getTableLayout(savedLayout.getId());

        assertThat(tableLayoutToCheck.getId()).isNotNull();
        assertThat(tableLayoutToCheck.getTotalCapacity()).isEqualTo(10);
        assertThat(tableLayoutToCheck.getTables()).hasSize(2);
        assertThat(tableLayoutToCheck.getTables()).satisfies(t -> assertThat(t.getId()).isEqualTo(tableLayoutToCheck.getId() + "-1"), Index.atIndex(0));

        assertThat(tableLayoutService.getTableLayouts(client)).hasSize(1);
    }

    @Test
    void testForDuplicateTableNames() {

        final TableLayout firstFloor = new TableLayout(client, "first floor");
        tableLayoutService.saveTableLayout(firstFloor);

        firstFloor.addTableDetails(new TableLayout.TableDetails("A", 1));
        firstFloor.addTableDetails(new TableLayout.TableDetails("B", 1));
        tableLayoutService.saveTableLayout(firstFloor);

        assertThatThrownBy(() -> {
            firstFloor.addTableDetails(new TableLayout.TableDetails("A", 1));
            tableLayoutService.saveTableLayout(firstFloor);
        }).isInstanceOf(ObjectAlreadyExistsException.class);

        final TableLayout.TableDetails firstTable = firstFloor.getTables().get(0);

        assertThatThrownBy(() -> {
            firstTable.setTableName("B");
            tableLayoutService.saveTableLayout(firstFloor);
        }).isInstanceOf(ObjectAlreadyExistsException.class);
    }
}