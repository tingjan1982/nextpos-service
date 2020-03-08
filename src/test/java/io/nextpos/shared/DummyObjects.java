package io.nextpos.shared;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.workingarea.data.WorkingArea;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DummyObjects {

    public static Client dummyClient() {
        return new Client("test", "admin@nextpos.io", "secret", "TW");
    }

    public static ClientUser dummyClientUser() {
        return new ClientUser(new ClientUser.ClientUserId("test-user", "admin@nextpos.io"), "password", "ADMIN,USER");
    }

    public static ProductVersion dummyProductVersion() {
        return dummyProductVersion("name");
    }

    public static ProductVersion dummyProductVersion(String productName) {
        return new ProductVersion(productName, "sku", "description", BigDecimal.ZERO);
    }

    public static ProductSnapshot productSnapshot() {
        final List<ProductSnapshot.ProductOptionSnapshot> productOptions = Collections.singletonList(new ProductSnapshot.ProductOptionSnapshot("ice", "half"));
        return new ProductSnapshot("pid", "coffee", "sku", BigDecimal.valueOf(100), productOptions);
    }

    public static ProductSnapshot productSnapshot(String name, BigDecimal price, ProductSnapshot.ProductOptionSnapshot... productOptions) {
        final List<ProductSnapshot.ProductOptionSnapshot> productOptionsList = productOptions != null ? Arrays.asList(productOptions) : List.of();
        return new ProductSnapshot("pid", name, "sku", price, productOptionsList);
    }

    public static ProductOptionVersion dummyProductOptionVersion() {
        return new ProductOptionVersion("option", ProductOptionVersion.OptionType.ONE_CHOICE, true);
    }

    public static WorkingArea dummyWorkingArea(Client client) {
        return new WorkingArea(client, "bar");
    }

    public static TableLayout dummyTableLayout(Client client) {

        final TableLayout tableLayout = new TableLayout(client, "dummy-table-layout");

        TableLayout.TableDetails tableDetails = new TableLayout.TableDetails("dummy-table", 5);
        tableLayout.addTableDetails(tableDetails);

        return tableLayout;
    }
}
