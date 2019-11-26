package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.storage.service.DistributedCounterService;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderCreationFactoryImpl implements OrderCreationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreationFactoryImpl.class);

    private final ProductService productService;

    private final SettingsService settingsService;

    private final TableLayoutService tableLayoutService;

    private final ClientSettingsService clientSettingsService;

    private final DistributedCounterService distributedCounterService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public OrderCreationFactoryImpl(final ProductService productService, final SettingsService settingsService, final TableLayoutService tableLayoutService, final ClientSettingsService clientSettingsService, final DistributedCounterService distributedCounterService, final OAuth2Helper oAuth2Helper) {
        this.productService = productService;
        this.settingsService = settingsService;
        this.tableLayoutService = tableLayoutService;
        this.clientSettingsService = clientSettingsService;
        this.distributedCounterService = distributedCounterService;
        this.oAuth2Helper = oAuth2Helper;
    }

    @Override
    public Order newOrder(final Client client, final OrderRequest orderRequest) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        final Order order = new Order(client.getId(), countrySettings.getTaxRate(), countrySettings.getCurrency());
        String serialId = generateSerialId();
        order.setSerialId(serialId);

        if (StringUtils.isNotEmpty(orderRequest.getTableId())) {
            tableLayoutService.getTableDetails(orderRequest.getTableId()).ifPresent(t -> order.setTableInfo(new Order.TableInfo(t.getId(), t.getTableName())));
        }

        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        order.setServedBy(clientUser.getName());

        order.setDemographicData(orderRequest.getDemographicData());

        if (!CollectionUtils.isEmpty(orderRequest.getLineItems())) {
            final List<OrderLineItem> orderLineItems = orderRequest.getLineItems().stream()
                    .map(li -> newOrderLineItem(client, li))
                    .collect(Collectors.toList());

            order.addOrderLineItems(orderLineItems);
        }

        clientSettingsService.getClientSettingByName(client, ClientSetting.SettingName.SERVICE_CHARGE).ifPresent(sc -> {
            if (sc.isEnabled()) {
                final BigDecimal serviceCharge = clientSettingsService.getActualStoredValue(sc, BigDecimal.class);
                order.setServiceCharge(serviceCharge);
            }
        });

        LOGGER.info("Created order: {}", order);

        return order;
    }

    private String generateSerialId() {
        final int counter = distributedCounterService.getNextRotatingCounter("order");
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + counter;
    }

    @Override
    public OrderLineItem newOrderLineItem(final Client client, final OrderLineItemRequest li) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());

        final Product product = productService.getProduct(li.getProductId());
        final ProductVersion productVersion = product.getDesignVersion();
        List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots = Collections.emptyList();

        if (!CollectionUtils.isEmpty(li.getProductOptions())) {
            productOptionSnapshots = li.getProductOptions().stream()
                    .map(po -> new ProductSnapshot.ProductOptionSnapshot(po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                    .collect(Collectors.toList());
        }

        final ProductSnapshot productSnapshot = new ProductSnapshot(product.getId(),
                productVersion.getProductName(),
                productVersion.getSku(),
                productVersion.getPrice(),
                productOptionSnapshots);

        if (product.getProductLabel() != null) {
            productSnapshot.setLabelInformation(product.getProductLabel().getId(), product.getProductLabel().getName());
        }

        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, li.getQuantity(), countrySettings.getTaxRate());

        if (product.getWorkingArea() != null) {
            orderLineItem.setWorkingAreaId(product.getWorkingArea().getId());
        }

        return orderLineItem;
    }
}
