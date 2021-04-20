package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ClientSubscriptionOrderServiceImplTest {

    private final ClientSubscriptionOrderService clientSubscriptionOrderService;

    private final ClientService clientService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final OrderService orderService;

    private final CountrySettings countrySettings;

    private Client client;

    private Client atlasClient;

    @Autowired
    ClientSubscriptionOrderServiceImplTest(ClientSubscriptionOrderService clientSubscriptionOrderService, ClientService clientService, InvoiceNumberRangeService invoiceNumberRangeService, OrderService orderService, CountrySettings countrySettings) {
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.clientService = clientService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.orderService = orderService;
        this.countrySettings = countrySettings;
    }

    @BeforeEach
    void prepare() {
        atlasClient = DummyObjects.dummyClient();
        atlasClient.addAttribute(Client.ClientAttributes.AES_KEY, "41BFE9D500D25491650E8B84C3EA3B3C");
        atlasClient.addAttribute(Client.ClientAttributes.UBN, "83515813");
        atlasClient.addAttribute(Client.ClientAttributes.COMPANY_NAME, "雨圖數位行銷科技股份有限公司");
        atlasClient.addAttribute(Client.ClientAttributes.ADDRESS, "台北市信義區基隆路二段12號");
        clientService.saveClient(atlasClient);

        client = new Client("Asian House", "tingjan1982@gmail.com", "123456", "TW", "Asia/Taipei");
        client.addAttribute(Client.ClientAttributes.UBN, "22640971");
        clientService.saveClient(client);

        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange("83515813", invoiceNumberRangeService.getCurrentRangeIdentifier(), "AG", "10011001", "10011002");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
    }

    @Test
    void sendClientSubscriptionOrder() {

        SubscriptionPlan subscriptionPlan = new SubscriptionPlan(countrySettings.getIsoCountryCode(), SubscriptionPlan.PlanGroup.DEFAULT, "Standard", countrySettings);
        subscriptionPlan.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(new BigDecimal("990")));
        ClientSubscription clientSubscription = new ClientSubscription(client.getId(), subscriptionPlan, SubscriptionPlan.PlanPeriod.MONTHLY);
        final ClientSubscriptionInvoice clientSubscriptionInvoice = new ClientSubscriptionInvoice(ZoneId.of("Asia/Taipei"), clientSubscription, new Date());

        clientSubscriptionOrderService.sendClientSubscriptionOrder(clientSubscriptionInvoice);

        final List<Order> orders = orderService.getOrders(atlasClient, ZonedDateRangeBuilder.builder(atlasClient, DateParameterType.TODAY).build());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0)).satisfies(o -> {
            assertThat(o.getState()).isEqualByComparingTo(Order.OrderState.COMPLETED);
            assertThat(o.getOrderTotal()).isEqualByComparingTo("990");
        });
    }
}