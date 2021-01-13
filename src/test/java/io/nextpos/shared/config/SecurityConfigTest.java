package io.nextpos.shared.config;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Set stateless to false on ResourceServer.
 * <p>
 * https://stackoverflow.com/questions/41824885/use-withmockuser-with-springboottest-inside-an-oauth2-resource-server-applic
 */
@SpringBootTest
@TestPropertySource(properties = "resourceServerStateless=false")
@ChainedTransaction
public class SecurityConfigTest {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    AuthorizationServerTokenServices tokenservice;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .dispatchOptions(true).build();
    }

    @Test
    void testClient() throws Exception {

        mockMvc.perform(get("/clientstatus/me")
                .with(authentication(createOAuth2Authentication("read:client"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/clientSettings")
                .with(authentication(createOAuth2Authentication("read:client"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/clientSettings")
                .with(authentication(createOAuth2Authentication("write:client"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testUserRole() throws Exception {

        mockMvc.perform(get("/roles")
                .with(authentication(createOAuth2Authentication("read:user_role"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/roles/someid")
                .with(authentication(createOAuth2Authentication("read:user_role"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/roles")
                .with(authentication(createOAuth2Authentication("write:user_role"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/roles/someid")
                .with(authentication(createOAuth2Authentication("write:user_role"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/roles/someid")
                .with(authentication(createOAuth2Authentication("delete:user_role"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testTimeCard() throws Exception {

        mockMvc.perform(get("/timecards/active")
                .with(authentication(createOAuth2Authentication("read:time_card"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/timecards/clockin")
                .with(authentication(createOAuth2Authentication("read:time_card"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testProduct() throws Exception {

        mockMvc.perform(get("/searches/products/grouped")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/products")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/products/someid")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/products")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/products/someid")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/products/someid/togglePin")
                .with(authentication(createOAuth2Authentication("write:product_toggles"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/products/someid/toggleOutOfStock")
                .with(authentication(createOAuth2Authentication("write:product_toggles"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/products/someid")
                .with(authentication(createOAuth2Authentication("delete:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testProductOption() throws Exception {

        mockMvc.perform(get("/productoptions")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/productoptions/someid")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/productoptions")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/productoptions/someid")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/productoptions/someid")
                .with(authentication(createOAuth2Authentication("delete:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testLabel() throws Exception {

        mockMvc.perform(get("/labels")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/labels/someid")
                .with(authentication(createOAuth2Authentication("read:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/labels")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/labels/someid")
                .with(authentication(createOAuth2Authentication("write:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/labels/someid")
                .with(authentication(createOAuth2Authentication("delete:product"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testOffer() throws Exception {

        mockMvc.perform(get("/offers")
                .with(authentication(createOAuth2Authentication("read:offer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/offers/someid")
                .with(authentication(createOAuth2Authentication("read:offer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/offers")
                .with(authentication(createOAuth2Authentication("write:offer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/offers/someid")
                .with(authentication(createOAuth2Authentication("write:offer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/offers/someid")
                .with(authentication(createOAuth2Authentication("delete:offer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testShift() throws Exception {

        mockMvc.perform(get("/shifts")
                .with(authentication(createOAuth2Authentication("read:shift"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/shifts/someid")
                .with(authentication(createOAuth2Authentication("read:shift"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/shifts/someid")
                .with(authentication(createOAuth2Authentication("write:shift"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testOrder() throws Exception {

        mockMvc.perform(get("/orders")
                .with(authentication(createOAuth2Authentication("read:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/orders/someid")
                .with(authentication(createOAuth2Authentication("read:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid")
                .with(authentication(createOAuth2Authentication("write:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/process")
                .with(authentication(createOAuth2Authentication("write:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/lineitems")
                .with(authentication(createOAuth2Authentication("write:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/lineitems/someid")
                .with(authentication(createOAuth2Authentication("write:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/applyDiscount")
                .with(authentication(createOAuth2Authentication("write:discount"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/removeDiscount")
                .with(authentication(createOAuth2Authentication("write:discount"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/orders/someid/waiveServiceCharge")
                .with(authentication(createOAuth2Authentication("write:discount"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/orders/someid/lineitems/someid")
                .with(authentication(createOAuth2Authentication("delete:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/orders/someid")
                .with(authentication(createOAuth2Authentication("delete:order"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testTableLayout() throws Exception {

        mockMvc.perform(get("/tablelayouts")
                .with(authentication(createOAuth2Authentication("read:table_layout"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/tablelayouts/someid")
                .with(authentication(createOAuth2Authentication("read:table_layout"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/tablelayouts")
                .with(authentication(createOAuth2Authentication("write:table_layout"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/tablelayouts/someid")
                .with(authentication(createOAuth2Authentication("write:table_layout"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/tablelayouts/someid")
                .with(authentication(createOAuth2Authentication("delete:table_layout"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testWorkingArea() throws Exception {

        mockMvc.perform(get("/workingareas")
                .with(authentication(createOAuth2Authentication("read:working_area"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/workingareas/someid")
                .with(authentication(createOAuth2Authentication("read:working_area"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/workingareas")
                .with(authentication(createOAuth2Authentication("write:working_area"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/workingareas/someid")
                .with(authentication(createOAuth2Authentication("write:working_area"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/workingareas/someid")
                .with(authentication(createOAuth2Authentication("delete:working_area"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testPrinter() throws Exception {

        mockMvc.perform(get("/printers")
                .with(authentication(createOAuth2Authentication("read:printer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/printers/someid")
                .with(authentication(createOAuth2Authentication("read:printer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/printers")
                .with(authentication(createOAuth2Authentication("write:printer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/printers/someid")
                .with(authentication(createOAuth2Authentication("write:printer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/printers/someid")
                .with(authentication(createOAuth2Authentication("delete:printer"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testAnnouncement() throws Exception {

        mockMvc.perform(get("/announcements")
                .with(authentication(createOAuth2Authentication("read:announcement"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/announcements/someid")
                .with(authentication(createOAuth2Authentication("read:announcement"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/announcements")
                .with(authentication(createOAuth2Authentication("write:announcement"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/announcements/someid")
                .with(authentication(createOAuth2Authentication("write:announcement"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/announcements/someid")
                .with(authentication(createOAuth2Authentication("delete:announcement"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testReport() throws Exception {

        mockMvc.perform(get("/reporting/rangedSalesReport")
                .with(authentication(createOAuth2Authentication("read:report"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    @Test
    void testMembership() throws Exception {

        mockMvc.perform(get("/memberships")
                .with(authentication(createOAuth2Authentication("read:membership"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(get("/memberships/someid")
                .with(authentication(createOAuth2Authentication("read:membership"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/memberships")
                .with(authentication(createOAuth2Authentication("write:membership"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(post("/memberships/someid")
                .with(authentication(createOAuth2Authentication("write:membership"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));

        mockMvc.perform(delete("/memberships/someid")
                .with(authentication(createOAuth2Authentication("delete:membership"))))
                .andDo(print())
                .andExpect(result -> assertNotEquals("Status", 403, result.getResponse().getStatus()));
    }

    private OAuth2Authentication createOAuth2Authentication(String scope) {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(scope));

        Map<String, String> requestParameters = Collections.emptyMap();
        Set<String> responseTypes = Collections.emptySet();
        Map<String, Serializable> extensionProperties = Collections.emptyMap();

        OAuth2Request oAuth2Request = new OAuth2Request(
                requestParameters,
                null,
                authorities,
                true,
                Set.of(scope),
                Set.of("nextpos-service"),
                null,
                responseTypes,
                extensionProperties);

        User userPrincipal = new User("user", "", true, true, true, true, authorities);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);

        return new OAuth2Authentication(oAuth2Request, authenticationToken);
    }
}
