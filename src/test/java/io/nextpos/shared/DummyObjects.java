package io.nextpos.shared;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.workingarea.data.WorkingArea;

import java.math.BigDecimal;
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

    public static ProductOptionVersion dummyProductOptionVersion() {
        return new ProductOptionVersion("option", ProductOptionVersion.OptionType.ONE_CHOICE, true);
    }

    public static WorkingArea dummyWorkingArea(Client client) {
        return new WorkingArea(client, "bar");
    }

    public static TableLayout dummyTableLayout(Client client) {

        TableLayout.TableDetails tableDetails = new TableLayout.TableDetails("dummy-table", 0, 0);
        final TableLayout tableLayout = new TableLayout(client, "dummy-table-layout", 5, 5);
        tableLayout.addTableDetails(tableDetails);

        return tableLayout;
    }
}
