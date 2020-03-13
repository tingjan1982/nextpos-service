package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.tablelayout.data.TableDetailsRepository;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.data.TableLayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TableLayoutServiceImpl implements TableLayoutService {

    private final TableLayoutRepository tableLayoutRepository;

    private final TableDetailsRepository tableDetailsRepository;

    @Autowired
    public TableLayoutServiceImpl(final TableLayoutRepository tableLayoutRepository, final TableDetailsRepository tableDetailsRepository) {
        this.tableLayoutRepository = tableLayoutRepository;
        this.tableDetailsRepository = tableDetailsRepository;
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
        return tableLayoutRepository.findAllByClientOrderByLayoutName(client);
    }

    @Override
    public void deleteTableLayout(final TableLayout tableLayout) {
        tableLayoutRepository.delete(tableLayout);
    }

    @Override
    public Optional<TableLayout.TableDetails> getTableDetails(final String id) {
        return tableDetailsRepository.findById(id);
    }

}
