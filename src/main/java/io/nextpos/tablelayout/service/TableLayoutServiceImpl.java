package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.data.TableLayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class TableLayoutServiceImpl implements TableLayoutService {

    private final TableLayoutRepository tableLayoutRepository;

    @Autowired
    public TableLayoutServiceImpl(final TableLayoutRepository tableLayoutRepository) {
        this.tableLayoutRepository = tableLayoutRepository;
    }

    @Override
    public TableLayout saveTableLayout(final TableLayout tableLayout) {
        return tableLayoutRepository.save(tableLayout);
    }

    @Override
    public TableLayout getTableLayout(final String id) {
        return tableLayoutRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, TableLayout.class);
        });
    }

    @Override
    public List<TableLayout> getTableLayouts(final Client client) {
        return tableLayoutRepository.findAllByClient(client);
    }
}
