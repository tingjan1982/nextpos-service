package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.roles.data.Permission;
import io.nextpos.roles.data.UserRole;
import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.web.RequestIdContextFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.*;

/**
 * The order of these Security components are:
 * Authorization Server -> Resource Server -> SecurityConfig
 * <p>
 * https://www.baeldung.com/spring-boot-security-autoconfiguration
 * <p>
 * https://www.baeldung.com/spring-security-oauth-jwt
 * <p>
 * Contains core security configuration and shared beans that are needed by the authorization server and resource server.
 * <p>
 * https://projects.spring.io/spring-security-oauth/docs/oauth2.html
 * <p>
 * https://docs.spring.io/spring-security-oauth2-boot/docs/current/reference/htmlsingle/
 */
@EnableWebSecurity(debug = false)
@Order(2)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${actuator.username}")
    private String actuatorUsername;

    @Value("${actuator.password}")
    private String actuatorPassword;

    private final ClientService clientService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(ClientService clientService, PasswordEncoder passwordEncoder) {
        this.clientService = clientService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Use basic auth only to authenticate /actuator and /counter requests.
     * <p>
     * Reference to configuring multiple HttpSecurity with @Order:
     * https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#multiple-httpsecurity
     * <p>
     * Regex online:
     * https://regex101.com/
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .cors().and()
                .regexMatcher("^\\/(actuator|counters|admin)(\\/.+)*(\\?.+)?$")
                .authorizeRequests()
                .antMatchers("/actuator/health", "/ws/**", "/admin/**").permitAll()
                .anyRequest().authenticated().and().httpBasic();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(clientService).passwordEncoder(passwordEncoder);

        final String encodedPassword = passwordEncoder.encode(actuatorPassword);
        auth.inMemoryAuthentication().withUser(actuatorUsername).password(encodedPassword).roles("ADMIN");
    }

    /**
     * Expose AuthenticationManager as a bean.
     *
     * @return
     * @throws Exception
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(OAuthSettings.SIGNING_KEY);
        converter.setAccessTokenConverter(new CustomAccessTokenConverter());

        return converter;
    }

    @Bean
    @Primary
    public TokenServicesWrapper tokenServices(@Qualifier("customEnhancer") TokenEnhancerChain tokenEnhancer) {
        TokenServicesWrapper defaultTokenServices = new TokenServicesWrapper();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setTokenEnhancer(tokenEnhancer);
        defaultTokenServices.setSupportRefreshToken(true);
        return defaultTokenServices;
    }


    static class CustomAccessTokenConverter extends DefaultAccessTokenConverter {

        @Override
        public OAuth2Authentication extractAuthentication(Map<String, ?> claims) {
            OAuth2Authentication authentication = super.extractAuthentication(claims);
            authentication.setDetails(new ExtraClaims(claims));

            return authentication;
        }
    }

    public static class ExtraClaims {

        private static final String APPLICATION_CLIENT_ID = "application_client_id";

        /**
         * OAuth2 token user name (if oauth token is obtained via password grant type)
         */
        private static final String USER_NAME = "user_name";

        private final Map<String, ?> claims;

        ExtraClaims(final Map<String, ?> claims) {
            this.claims = claims;
        }

        public Map<String, ?> getClaims() {
            return claims;
        }

        /**
         * Used in ClientResolver to resolve the Client object referenced by access token for access control check.
         */
        public String getApplicationClientId() {
            return (String) claims.get(APPLICATION_CLIENT_ID);
        }

        public String getOAuth2TokenUsername() {
            return ((String) claims.get(USER_NAME));
        }
    }


    /**
     * Authorization server configuration
     */
    @Component
    @EnableAuthorizationServer
    public static class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

        private final TokenStore tokenStore;

        private final JwtAccessTokenConverter accessTokenConverter;

        private final AuthenticationManager authenticationManager;

        private final JdbcClientDetailsService clientDetailsService;

        private final ClientService clientService;


        @Autowired
        public AuthorizationServer(TokenStore tokenStore, JwtAccessTokenConverter accessTokenConverter, AuthenticationManager authenticationManager, JdbcClientDetailsService clientDetailsService, ClientService clientService) {
            this.tokenStore = tokenStore;
            this.accessTokenConverter = accessTokenConverter;
            this.authenticationManager = authenticationManager;
            this.clientDetailsService = clientDetailsService;
            this.clientService = clientService;
        }


        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

            final TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
            final ClientTokenEnhancer clientTokenEnhancer = new ClientTokenEnhancer(clientService);
            enhancerChain.setTokenEnhancers(Arrays.asList(clientTokenEnhancer, accessTokenConverter));
            final DefaultOAuth2RequestFactory oAuth2RequestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
            oAuth2RequestFactory.setCheckUserScopes(true);

            endpoints.tokenStore(tokenStore)
                    .tokenEnhancer(enhancerChain)
                    .authenticationManager(authenticationManager)
                    .requestFactory(oAuth2RequestFactory)
                    .userDetailsService(clientService);
        }

        @Bean("customEnhancer")
        public TokenEnhancerChain TokenEnhancerChain() {
            final TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
            final ClientTokenEnhancer clientTokenEnhancer = new ClientTokenEnhancer(clientService);
            enhancerChain.setTokenEnhancers(Arrays.asList(clientTokenEnhancer, accessTokenConverter));
            final DefaultOAuth2RequestFactory oAuth2RequestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
            oAuth2RequestFactory.setCheckUserScopes(true);

            return enhancerChain;
        }

        @Override
        public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(clientDetailsService);
        }


        static class ClientTokenEnhancer implements TokenEnhancer {

            private final ClientService clientService;

            ClientTokenEnhancer(final ClientService clientService) {
                this.clientService = clientService;
            }

            @Override
            public OAuth2AccessToken enhance(final OAuth2AccessToken accessToken, final OAuth2Authentication authentication) {

                final String clientId = authentication.getOAuth2Request().getClientId();
                final Client client = clientService.getClientByUsername(clientId).orElseThrow(() -> {
                    throw new ConfigurationException("Client is not resolvable by username. The token cannot be enhanced: " + clientId);
                });

                final Map<String, Object> additionalInfo = Map.of(ExtraClaims.APPLICATION_CLIENT_ID, client.getId());
                final DefaultOAuth2AccessToken oauth2AccessToken = (DefaultOAuth2AccessToken) accessToken;
                oauth2AccessToken.setAdditionalInformation(additionalInfo);
                final Set<String> actualScopes = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
                oauth2AccessToken.setScope(actualScopes);

                return accessToken;
            }
        }
    }

    @Component
    @EnableResourceServer
    public static class ResourceServer extends ResourceServerConfigurerAdapter {

        private final ResourceServerTokenServices tokenServices;

        private final RequestIdContextFilter requestIdContextFilter;

        @Value("${resourceServerStateless}")
        private boolean resourceServerStateless;

        @Autowired
        public ResourceServer(final ResourceServerTokenServices tokenService, final RequestIdContextFilter requestIdContextFilter) {
            this.tokenServices = tokenService;
            this.requestIdContextFilter = requestIdContextFilter;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer config) {
            config.tokenServices(tokenServices)
                    .resourceId(OAuthSettings.RESOURCE_ID)
                    .stateless(resourceServerStateless);
        }

        /**
         * For more supported expression based access control, see: OAuth2SecurityExpressionMethods
         * <p>
         * For adding filter in the security filter chain, see:
         * https://www.baeldung.com/spring-security-registered-filters
         * https://www.baeldung.com/spring-security-custom-filter
         * https://stackoverflow.com/questions/44651573/how-to-add-filter-before-my-another-filter-in-spring-security?noredirect=1&lq=1
         *
         * @param http
         * @throws Exception
         */
        @Override
        public void configure(HttpSecurity http) throws Exception {

            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            http.csrf().disable()
                    .cors().and()
                    .addFilterBefore(requestIdContextFilter, WebAsyncManagerIntegrationFilter.class)
                    .authorizeRequests()
                    .antMatchers("/account/**", "/error", "/favicon.ico", "/ws/**").permitAll();

            this.authorizeClientRequests(http);
            this.authorizeClientStatusRequests(http);
            this.authorizeUserRoleRequests(http);
            this.authorizeLinkedClientAccountRequests(http);
            this.authorizeTimeCardRequests(http);
            this.authorizeTablesAndWorkingAreaRequests(http);
            this.authorizeProductRequests(http);
            this.authorizeShiftAndOrderRequests(http);
            this.authorizeAnnouncementRequests(http);
            this.authorizeReportingRequests(http);
            this.authorizeInvoiceNumberRequests(http);
            this.authorizeMembershipRequests(http);
            this.authorizeClientSubscriptionRequests(http);
            this.authorizeRosterPlanRequests(http);
            this.authorizeInventoryRequests(http);

            http.authorizeRequests().anyRequest().authenticated();
        }

        private void authorizeClientRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(POST, "/clients/*/deactivate").access("hasAuthority('MASTER')")
                    .antMatchers(POST, "/clients/resetPassword").access("hasAuthority('MASTER')")
                    .antMatchers(DELETE, "/clients/*/hard").access("hasAuthority('MASTER')")
                    .antMatchers(POST, "/clients").permitAll()
                    .antMatchers(GET, "/clients/default").permitAll();

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/clients/me/users/**", UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/clients/me/users/**", UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.WRITE), Role.OWNER_ROLE)
                    .addAuthorization(PATCH, "/clients/me/users/currentUser/password", UserRole.UserPermission.of(Permission.CURRENT_USER, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(PATCH, "/clients/me/users/*/password", UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.WRITE), Role.OWNER_ROLE)
                    .addAuthorization(DELETE, "/clients/me/users/*", UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.DELETE), Role.OWNER_ROLE)

                    .addAuthorization(GET, "/clients/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/clients/me/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.WRITE), Role.OWNER_ROLE)
                    .addAuthorization(DELETE, "/clients/me", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.DELETE), Role.ADMIN_ROLE)
                    .decorate();
        }

        private void authorizeClientStatusRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization("/clientstatus/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(GET, "/clientSettings/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/clientSettings/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.WRITE), Role.OWNER_ROLE)
                    .decorate();
        }

        private void authorizeUserRoleRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/roles/**", UserRole.UserPermission.of(Permission.USER_ROLE, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/roles/**", UserRole.UserPermission.of(Permission.USER_ROLE, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(DELETE, "/roles/**", UserRole.UserPermission.of(Permission.USER_ROLE, Permission.Operation.DELETE), Role.USER_ROLE)
                    .decorate();
        }

        private void authorizeLinkedClientAccountRequests(HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(GET, "/linkedClientAccounts/**").hasAuthority(Role.OWNER_ROLE);
        }

        private void authorizeTimeCardRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization("/timecards/**", UserRole.UserPermission.of(Permission.TIME_CARD, Permission.Operation.ALL), Role.USER_ROLE)
                    .decorate();
        }

        private void authorizeTablesAndWorkingAreaRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/tablelayouts/**", UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/tablelayouts/**", UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/tablelayouts/**", UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/workingareas/**", UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/workingareas/**", UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/workingareas/**", UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/printers/**", UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/printers/**", UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/printers/**", UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.DELETE), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeProductRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/products/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(GET, "/searchces/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/products/*/togglePin", UserRole.UserPermission.of(Permission.PRODUCT_TOGGLES, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(POST, "/products/*/toggleOutOfStock", UserRole.UserPermission.of(Permission.PRODUCT_TOGGLES, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(POST, "/products/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/products/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/productoptions/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/productoptions/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/productoptions/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/labels/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/labels/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/labels/**", UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/offers/**", UserRole.UserPermission.of(Permission.OFFER, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/offers/**", UserRole.UserPermission.of(Permission.OFFER, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/offers/**", UserRole.UserPermission.of(Permission.OFFER, Permission.Operation.DELETE), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeShiftAndOrderRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/shifts/**", UserRole.UserPermission.of(Permission.SHIFT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/shifts/**", UserRole.UserPermission.of(Permission.SHIFT, Permission.Operation.WRITE), Role.USER_ROLE)

                    .addAuthorization(POST, "/orders/*/applyDiscount", UserRole.UserPermission.of(Permission.DISCOUNT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(POST, "/orders/*/removeDiscount", UserRole.UserPermission.of(Permission.DISCOUNT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(POST, "/orders/*/waiveServiceCharge", UserRole.UserPermission.of(Permission.DISCOUNT, Permission.Operation.WRITE), Role.MANAGER_ROLE)

                    .addAuthorization(GET, "/orders/**", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/orders/**", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(DELETE, "/orders/*/lineitems/*", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.DELETE), Role.USER_ROLE)
                    .addAuthorization(DELETE, "/orders/**", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.DELETE), Role.MANAGER_ROLE)

                    .addAuthorization("/ordersets/**", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.ALL), Role.USER_ROLE)
                    .addAuthorization("/splitOrders/**", UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.ALL), Role.USER_ROLE)
                    .decorate();
        }

        private void authorizeAnnouncementRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/announcements/**", UserRole.UserPermission.of(Permission.ANNOUNCEMENT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/announcements/**", UserRole.UserPermission.of(Permission.ANNOUNCEMENT, Permission.Operation.WRITE), Role.MANAGER_ROLE)
                    .addAuthorization(DELETE, "/announcements/**", UserRole.UserPermission.of(Permission.ANNOUNCEMENT, Permission.Operation.DELETE), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeReportingRequests(final HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/reporting/**", UserRole.UserPermission.of(Permission.REPORT, Permission.Operation.READ), Role.OWNER_ROLE)
                    .decorate();
        }

        private void authorizeInvoiceNumberRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization("/einvoices/**", UserRole.UserPermission.of(Permission.EINVOICE, Permission.Operation.ALL), Role.MANAGER_ROLE)
                    .addAuthorization("/invoiceNumbers/**", UserRole.UserPermission.of(Permission.EINVOICE, Permission.Operation.ALL), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeMembershipRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/memberships/**", UserRole.UserPermission.of(Permission.MEMBERSHIP, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/memberships/**", UserRole.UserPermission.of(Permission.MEMBERSHIP, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(DELETE, "/memberships/**", UserRole.UserPermission.of(Permission.MEMBERSHIP, Permission.Operation.DELETE), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeClientSubscriptionRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/clientSubscriptions/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/clientSubscriptions/**", UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.WRITE), Role.OWNER_ROLE)
                    .decorate();
        }

        private void authorizeRosterPlanRequests(HttpSecurity http) throws Exception {

            HttpSecurityDecorator.newInstance(http)
                    .addAuthorization(GET, "/rosterEvents/**", UserRole.UserPermission.of(Permission.ROSTER, Permission.Operation.READ), Role.USER_ROLE)
                    .addAuthorization(POST, "/rosterEvents/**", UserRole.UserPermission.of(Permission.ROSTER, Permission.Operation.WRITE), Role.USER_ROLE)
                    .addAuthorization(DELETE, "/rosterEvents/**", UserRole.UserPermission.of(Permission.ROSTER, Permission.Operation.DELETE), Role.MANAGER_ROLE)
                    .decorate();
        }

        private void authorizeInventoryRequests(HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers("/inventories/**").hasAuthority(Role.MANAGER_ROLE);
        }
    }

    public static class OAuthSettings {

        public static final String RESOURCE_ID = "nextpos-service";

        private static final String SIGNING_KEY = "1qaz2wsx";
    }

    public interface Role {

        String MASTER_ROLE = "MASTER";

        String ADMIN_ROLE = "ADMIN";

        String OWNER_ROLE = "OWNER";

        String MANAGER_ROLE = "MANAGER";

        String USER_ROLE = "USER";

        static List<String> getRoles() {
            return Arrays.asList(ADMIN_ROLE, OWNER_ROLE, MANAGER_ROLE, USER_ROLE);
        }
    }
}
