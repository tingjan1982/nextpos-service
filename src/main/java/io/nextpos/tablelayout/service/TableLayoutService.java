package io.nextpos.tablelayout.service;

import io.nextpos.client.data.Client;
import io.nextpos.tablelayout.data.TableLayout;

import java.util.List;

public interface TableLayoutService {

    TableLayout saveTableLayout(TableLayout tableLayout);

    TableLayout getTableLayout(String id);

    List<TableLayout> getTableLayouts(Client client);
}
