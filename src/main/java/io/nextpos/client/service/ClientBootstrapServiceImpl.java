package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@ChainedTransaction
public class ClientBootstrapServiceImpl implements ClientBootstrapService {

    private final TableLayoutService tableLayoutService;

    private final WorkingAreaService workingAreaService;

    private final ProductLabelService productLabelService;

    private final ProductService productService;

    @Autowired
    public ClientBootstrapServiceImpl(TableLayoutService tableLayoutService, WorkingAreaService workingAreaService, ProductLabelService productLabelService, ProductService productService) {
        this.tableLayoutService = tableLayoutService;
        this.workingAreaService = workingAreaService;
        this.productLabelService = productLabelService;
        this.productService = productService;
    }

    @Override
    public void bootstrapClient(Client client) {

        final TableLayout firstFloor = new TableLayout(client, "1F");
        firstFloor.addTableDetails(new TableLayout.TableDetails("A1", 4));
        firstFloor.addTableDetails(new TableLayout.TableDetails("A2", 4));
        firstFloor.addTableDetails(new TableLayout.TableDetails("A3", 4));

        tableLayoutService.saveTableLayout(firstFloor);

        final WorkingArea workingArea = new WorkingArea(client, "出餐區");
        workingAreaService.saveWorkingArea(workingArea);

        final ProductLabel drinks = new ProductLabel("飲品 (預設)", client);
        final ProductLabel mains = new ProductLabel("主餐 (預設)", client);
        final ProductLabel desserts = new ProductLabel("甜品 (預設)", client);

        productLabelService.saveProductLabel(drinks);
        productLabelService.saveProductLabel(mains);
        productLabelService.saveProductLabel(desserts);

        final Product coffee = Product.builder(client).productNameAndPrice("美式咖啡", new BigDecimal("50")).build();
        coffee.setProductLabel(drinks);
        coffee.setWorkingArea(workingArea);

        productService.saveProduct(coffee);

        final Product pasta = Product.builder(client).productNameAndPrice("義大利麵", new BigDecimal("250")).build();
        pasta.setProductLabel(mains);
        pasta.setWorkingArea(workingArea);

        productService.saveProduct(pasta);

        final Product cake = Product.builder(client).productNameAndPrice("好吃的蛋糕", new BigDecimal("85")).build();
        cake.setProductLabel(mains);
        cake.setWorkingArea(workingArea);

        productService.saveProduct(cake);
    }
}
