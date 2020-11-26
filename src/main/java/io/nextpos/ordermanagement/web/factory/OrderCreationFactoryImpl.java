package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductSet;
import io.nextpos.product.service.ProductService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderCreationFactoryImpl implements OrderCreationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreationFactoryImpl.class);

    private final ProductService productService;

    private final SettingsService settingsService;

    private final TableLayoutService tableLayoutService;

    private final ClientSettingsService clientSettingsService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public OrderCreationFactoryImpl(final ProductService productService, final SettingsService settingsService, final TableLayoutService tableLayoutService, final ClientSettingsService clientSettingsService, final OAuth2Helper oAuth2Helper) {
        this.productService = productService;
        this.settingsService = settingsService;
        this.tableLayoutService = tableLayoutService;
        this.clientSettingsService = clientSettingsService;
        this.oAuth2Helper = oAuth2Helper;
    }

    @Override
    public Order newOrder(final Client client, final OrderRequest orderRequest) {

        final OrderSettings orderSettings = createOrderSettings(client);

        final Order order = Order.newOrder(client.getId(), orderRequest.getOrderType(), orderSettings);
        updateTableInfoAndDemographicData(order, orderRequest);

        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        order.setServedBy(clientUser.getName());

        if (!CollectionUtils.isEmpty(orderRequest.getLineItems())) {
            final List<OrderLineItem> orderLineItems = orderRequest.getLineItems().stream()
                    .map(li -> newOrderLineItem(client, li))
                    .collect(Collectors.toList());

            order.addOrderLineItems(orderLineItems);
        }

        LOGGER.info("Created order: {}", order);

        return order;
    }

    @Override
    public void updateTableInfoAndDemographicData(final Order order, final OrderRequest orderRequest) {

        updateTableInfo(order, orderRequest);

        if (orderRequest.getDemographicData() != null) {
            order.setDemographicData(orderRequest.getDemographicData());
        }
    }

    private void updateTableInfo(Order order, OrderRequest orderRequest) {

        order.setOrderType(orderRequest.getOrderType());

        if (orderRequest.getOrderType() == Order.OrderType.IN_STORE) {
            Set<String> tableIds = new HashSet<>(orderRequest.getTableIds());

            final List<TableLayout.TableDetails> tables = tableIds.stream()
                    .map(tableLayoutService::getTableDetailsOrThrows)
                    .collect(Collectors.toList());

            order.updateTables(tables);

            if (order.isTablesEmpty()) {
                throw new BusinessLogicException("message.emptyTables", "There must at least be one table associated with an in-store order");
            }
        }
    }

    @Override
    public OrderLineItem newOrderLineItem(final Client client, final OrderLineItemRequest li) {

        final Product product = productService.getProduct(li.getProductId());

        if (product.isOutOfStock()) {
            throw new BusinessLogicException("message.outOfStock", "Product is marked as out of stock: " + product.getId());
        }

        List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots = Collections.emptyList();

        if (!CollectionUtils.isEmpty(li.getProductOptions())) {
            productOptionSnapshots = li.getProductOptions().stream()
                    .map(po -> new ProductSnapshot.ProductOptionSnapshot(po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                    .collect(Collectors.toList());
        }

        final ProductSnapshot productSnapshot = new ProductSnapshot(product);
        productSnapshot.setProductOptions(productOptionSnapshots);

        if (product instanceof ProductSet) {
            final List<ProductSnapshot.ChildProductSnapshot> childProducts = ((ProductSet) product).getChildProducts().stream()
                    .map(p -> new ProductSnapshot.ChildProductSnapshot(p.getId(),
                            p.getDesignVersion().getProductName(),
                            p.getDesignVersion().getInternalProductName()))
                    .collect(Collectors.toList());

            productSnapshot.setChildProducts(childProducts);
        }

        if (li.getOverridePrice().compareTo(BigDecimal.ZERO) > 0) {
            productSnapshot.setOverridePrice(li.getOverridePrice());
        }

        if (product.getProductLabel() != null) {
            productSnapshot.setLabelInformation(product.getProductLabel().getId(), product.getProductLabel().getName());
        }

        final OrderSettings orderSettings = createOrderSettings(client);
        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, li.getQuantity(), orderSettings);

        if (product.getWorkingArea() != null) {
            orderLineItem.setWorkingAreaId(product.getWorkingArea().getId());
        }

        return orderLineItem;
    }

    private OrderSettings createOrderSettings(final Client client) {
        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        final OrderSettings orderSettings = new OrderSettings(countrySettings, countrySettings.getTaxInclusive(), BigDecimal.ZERO);

        clientSettingsService.getClientSettingByName(client, ClientSetting.SettingName.TAX_INCLUSIVE).ifPresent(cs -> {
            final Boolean taxInclusive = clientSettingsService.getActualStoredValue(cs, Boolean.class);
            orderSettings.setTaxInclusive(taxInclusive);
        });

        clientSettingsService.getClientSettingByName(client, ClientSetting.SettingName.SERVICE_CHARGE).ifPresent(sc -> {
            if (sc.isEnabled()) {
                final BigDecimal serviceCharge = clientSettingsService.getActualStoredValue(sc, BigDecimal.class);
                orderSettings.setServiceCharge(serviceCharge);
            }
        });

        return orderSettings;
    }
}
