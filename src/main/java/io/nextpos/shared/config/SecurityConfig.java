package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.web.RequestIdContextFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
                .regexMatcher("^\\/(actuator|counters|admin)(\\/[\\w.+]*)*$")
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
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
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


    @EnableResourceServer
    public static class ResourceServer extends ResourceServerConfigurerAdapter {

        private final ResourceServerTokenServices tokenServices;

        private final RequestIdContextFilter requestIdContextFilter;

        @Autowired
        public ResourceServer(final ResourceServerTokenServices tokenService, final RequestIdContextFilter requestIdContextFilter) {
            this.tokenServices = tokenService;
            this.requestIdContextFilter = requestIdContextFilter;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer config) {
            config.tokenServices(tokenServices)
                    .resourceId(OAuthSettings.RESOURCE_ID)
                    .stateless(true);
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
                    .antMatchers("/clients/default", "/account/**", "/error", "/favicon.ico", "/ws/**").permitAll();

            this.authorizeClientRequests(http);
            this.authorizeTimeCardRequests(http);
            this.authorizeTablesAndWorkingAreaRequests(http);
            this.authorizeProductRequests(http);
            this.authorizeShiftAndOrderRequests(http);
            this.authorizeAnnouncementRequests(http);
            this.authorizeReportingRequests(http);
            this.authorizeInvoiceNumberRequests(http);

            http.authorizeRequests().anyRequest().authenticated();

//                    .antMatchers(HttpMethod.POST, "/clients/me/users").access("hasAuthority('ADMIN') and #oauth2.hasScopeMatching('client:write')")
//                    .antMatchers(HttpMethod.GET, "/products/**", "/productoptions/**").access("hasAnyAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
//                    .antMatchers(HttpMethod.POST, "/products/**", "/productoptions/**").access("hasAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
        }

        private void authorizeClientRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(POST, "/clients/*/deactivate").access("hasAuthority('MASTER')")
                    .antMatchers(POST, "/clients/resetPassword").access("hasAuthority('MASTER')")
                    .antMatchers(DELETE, "/clients/*/hard").access("hasAuthority('MASTER')")
                    .antMatchers(POST, "/clients").permitAll()
                    .antMatchers(GET, "/clients/default").permitAll()
                    .antMatchers(GET, "/clients/me", "/clients/*").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/clients/me").hasAuthority(Role.ADMIN_ROLE)
                    .antMatchers(DELETE, "/clients/me").hasAuthority(Role.ADMIN_ROLE)
                    .antMatchers(GET, "/clients/me/users").hasAuthority(Role.USER_ROLE)
                    .antMatchers(GET, "/clients/me/users/*").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/clients/me/users/*").hasAuthority(Role.OWNER_ROLE)
                    .antMatchers(PATCH, "/clients/me/users/currentUser/password").hasAuthority(Role.USER_ROLE)
                    .antMatchers(PATCH, "/clients/me/users/**").hasAuthority(Role.OWNER_ROLE)
                    .antMatchers(DELETE, "/clients/me/users/*").hasAuthority(Role.OWNER_ROLE);
        }

        private void authorizeTimeCardRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers("/timecards/**").hasAuthority(Role.USER_ROLE);
        }

        private void authorizeTablesAndWorkingAreaRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(GET, "/tablelayouts/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/tablelayouts/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(DELETE, "/tablelayouts/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(GET, "/workingareas/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/workingareas/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(GET, "/printers/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/printers/**").hasAuthority(Role.MANAGER_ROLE);
        }

        private void authorizeProductRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(GET, "/products/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/products/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(DELETE, "/products/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(GET, "/productoptions/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/productoptions/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(DELETE, "/productoptions/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(GET, "/labels/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/labels/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers("/searches/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(GET,"/offers/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST,"/offers/**").hasAuthority(Role.MANAGER_ROLE)
                    .antMatchers(DELETE,"/offers/**").hasAuthority(Role.MANAGER_ROLE);
        }

        private void authorizeShiftAndOrderRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(POST, "/orders/*/applyDiscount").access("#oauth2.hasScope('write:discount') or hasAuthority('MANAGER')")
                    .antMatchers("/shifts/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(GET, "/orders/**").hasAuthority(Role.USER_ROLE)
                    .regexMatchers(POST, "\\/orders(\\/?((?!applyDiscount).)+)").hasAuthority(Role.USER_ROLE)
                    .antMatchers(DELETE, "/orders/**").hasAuthority(Role.MANAGER_ROLE);
        }

        private void authorizeAnnouncementRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers(GET, "/announcements/**").hasAuthority(Role.USER_ROLE)
                    .antMatchers(POST, "/announcements/**").hasAuthority(Role.MANAGER_ROLE);
        }

        private void authorizeReportingRequests(final HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers("/reporting/**").hasAuthority(Role.OWNER_ROLE);
        }

        private void authorizeInvoiceNumberRequests(HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers("/invoiceNumbers/**").hasAuthority(Role.OWNER_ROLE);
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
