package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.shared.DummyObjects;
import io.nextpos.tablelayout.data.TableLayout;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
}