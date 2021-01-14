package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.roles.data.PermissionBundle;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.service.UserRoleService;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@ChainedTransaction
public class ClientBootstrapServiceImpl implements ClientBootstrapService {

    private final TableLayoutService tableLayoutService;

    private final WorkingAreaService workingAreaService;

    private final ProductLabelService productLabelService;

    private final ProductService productService;

    private final UserRoleService userRoleService;

    @Autowired
    public ClientBootstrapServiceImpl(TableLayoutService tableLayoutService, WorkingAreaService workingAreaService, ProductLabelService productLabelService, ProductService productService, UserRoleService userRoleService) {
        this.tableLayoutService = tableLayoutService;
        this.workingAreaService = workingAreaService;
        this.productLabelService = productLabelService;
        this.productService = productService;
        this.userRoleService = userRoleService;
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

        bootstrapUserRoles(client);
    }

    @Override
    public Map<String, UserRole> bootstrapUserRoles(Client client) {

        final UserRole staff = new UserRole(client, "員工");
        staff.addPermissionBundle(PermissionBundle.CREATE_ORDER);

        userRoleService.saveUserRole(staff);

        final UserRole supervisor = new UserRole(client, "店長");
        supervisor.addPermissionBundle(PermissionBundle.CREATE_ORDER);
        supervisor.addPermissionBundle(PermissionBundle.DELETE_ORDER);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_SHIFT);
        supervisor.addPermissionBundle(PermissionBundle.APPLY_DISCOUNT);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_MEMBERSHIP);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_STAFF);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_ROLE);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_PRODUCT);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_SETTINGS);
        supervisor.addPermissionBundle(PermissionBundle.MANAGE_ANNOUNCEMENT);

        userRoleService.saveUserRole(supervisor);

        final UserRole manager = new UserRole(client, "主管");
        manager.addPermissionBundle(PermissionBundle.CREATE_ORDER);
        manager.addPermissionBundle(PermissionBundle.DELETE_ORDER);
        manager.addPermissionBundle(PermissionBundle.MANAGE_SHIFT);
        manager.addPermissionBundle(PermissionBundle.APPLY_DISCOUNT);
        manager.addPermissionBundle(PermissionBundle.MANAGE_MEMBERSHIP);
        manager.addPermissionBundle(PermissionBundle.MANAGE_STAFF);
        manager.addPermissionBundle(PermissionBundle.MANAGE_ROLE);
        manager.addPermissionBundle(PermissionBundle.MANAGE_PRODUCT);
        manager.addPermissionBundle(PermissionBundle.MANAGE_SETTINGS);
        manager.addPermissionBundle(PermissionBundle.MANAGE_ANNOUNCEMENT);
        manager.addPermissionBundle(PermissionBundle.MANAGE_STORE);
        manager.addPermissionBundle(PermissionBundle.MANAGE_EINVOICE);
        manager.addPermissionBundle(PermissionBundle.VIEW_REPORT);
        manager.addPermissionBundle(PermissionBundle.MANAGE_ROSTER);

        userRoleService.saveUserRole(manager);

        return Map.of(staff.getName(), staff, supervisor.getName(), supervisor, manager.getName(), manager);
    }
}
