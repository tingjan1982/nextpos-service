package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientStatus;
import io.nextpos.client.data.ClientStatusRepository;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import java.util.List;

@Service
@ChainedTransaction
public class ClientStatusServiceImpl implements ClientStatusService {

    private final ClientStatusRepository clientStatusRepository;

    private final TableLayoutService tableLayoutService;

    private final ElectronicInvoiceService electronicInvoiceService;

    private final EntityManager entityManager;


    @Autowired
    public ClientStatusServiceImpl(ClientStatusRepository clientStatusRepository, TableLayoutService tableLayoutService, ElectronicInvoiceService electronicInvoiceService, EntityManager entityManager) {
        this.clientStatusRepository = clientStatusRepository;
        this.tableLayoutService = tableLayoutService;
        this.electronicInvoiceService = electronicInvoiceService;
        this.entityManager = entityManager;
    }

    @Override
    public ClientStatus checkClientStatus(Client client) {

        final ClientStatus clientStatus = clientStatusRepository.findById(client.getId()).orElseGet(() -> {
            final ClientStatus newClientStatus = new ClientStatus();
            newClientStatus.setId(client.getId());
            newClientStatus.setClient(client);

            return newClientStatus;
        });

        final List<TableLayout> tableLayouts = tableLayoutService.getTableLayouts(client);
        clientStatus.setNoTableLayout(CollectionUtils.isEmpty(tableLayouts));

        if (CollectionUtils.isEmpty(tableLayouts)) {
            clientStatus.setNoTable(true);
        } else {
            final boolean hasTable = tableLayouts.stream().anyMatch(tl -> !CollectionUtils.isEmpty(tl.getTables()));
            clientStatus.setNoTable(!hasTable);
        }

        clientStatus.setNoCategory(hasNoRecord(client, ProductLabel.class));
        clientStatus.setNoProduct(hasNoRecord(client, Product.class));
        clientStatus.setNoWorkingArea(hasNoRecord(client, WorkingArea.class));
        clientStatus.setNoPrinter(hasNoRecord(client, Printer.class));

        final boolean einvoiceEligible = electronicInvoiceService.checkElectronicInvoiceEligibility(client);
        clientStatus.setNoElectronicInvoice(!einvoiceEligible);

        return clientStatusRepository.save(clientStatus);
    }

    private boolean hasNoRecord(Client client, Class<?> domainClass) {

        final JpaEntityInformation<?, ?> entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        final String tableName = entityInformation.getEntityName();

        final String countQuery = String.format("select count(x) from %s x where client_id = '%s'", tableName, client.getId());
        final Long count = entityManager.createQuery(countQuery, Long.class).getSingleResult();

        return count == 0;
    }
}
