package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.service.ClientServiceImpl;
import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.web.RequestIdContextFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
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
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * https://www.baeldung.com/spring-boot-security-autoconfiguration
 * <p>
 * https://www.baeldung.com/spring-security-oauth-jwt
 * <p>
 * Contains core security configuration and shared beans that are needed by the authorization server and resource server.
 * <p>
 * https://projects.spring.io/spring-security-oauth/docs/oauth2.html
 */
@EnableWebSecurity(debug = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {


//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//
//        http.csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/actuator/**").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .httpBasic();
//    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

        auth.userDetailsService(clientService()).passwordEncoder(passwordEncoder());
        //auth.inMemoryAuthentication().withUser("admin").password(passwordEncoder().encode("admin")).roles("ADMIN");
    }

    /**
     * This was done instead of the usual constructor injection is to circumvent the circular reference issue
     * during startup as ClientServiceImpl depends on JdbcClientDetailsService which is initialized in another class.
     */
    @Lookup
    public ClientServiceImpl clientService() {
        return null;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    class CustomAccessTokenConverter extends DefaultAccessTokenConverter {

        @Override
        public OAuth2Authentication extractAuthentication(Map<String, ?> claims) {
            OAuth2Authentication authentication = super.extractAuthentication(claims);
            authentication.setDetails(new ExtraClaims(claims));

            return authentication;
        }
    }

    public static class ExtraClaims {

        private static final String CLIENT_ID = "clientId";

        private final Map<String, ?> claims;

        ExtraClaims(final Map<String, ?> claims) {
            this.claims = claims;
        }

        public Map<String, ?> getClaims() {
            return claims;
        }

        public String getClientId() {
            return (String) claims.get(CLIENT_ID);
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


        @Autowired
        public AuthorizationServer(final DataSource dataSource, final TokenStore tokenStore, final JwtAccessTokenConverter accessTokenConverter, final AuthenticationManager authenticationManager, final PasswordEncoder passwordEncoder) {
            this.tokenStore = tokenStore;
            this.accessTokenConverter = accessTokenConverter;
            this.authenticationManager = authenticationManager;

            clientDetailsService = new JdbcClientDetailsService(dataSource);
            clientDetailsService.setPasswordEncoder(passwordEncoder);
        }


        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

            final TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
            final ClientTokenEnhancer clientTokenEnhancer = new ClientTokenEnhancer(clientService());
            enhancerChain.setTokenEnhancers(Arrays.asList(clientTokenEnhancer, accessTokenConverter));

            endpoints.tokenStore(tokenStore)
                    .tokenEnhancer(enhancerChain)
                    .authenticationManager(authenticationManager);
        }

        @Override
        public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(jdbcClientDetailsService());
        }

        @Lookup
        public ClientServiceImpl clientService() {
            return null;
        }

        /**
         * This bean is exposed early for ClientServiceImpl to bootstrap the registration of test client.
         * It will also later be used in ClientDetailsServiceConfiguration to expose it as a bean, which is by default
         * defined as lazy.
         *
         * @return
         */
        @Bean("jdbcClientDetailsService")
        public JdbcClientDetailsService jdbcClientDetailsService() {
            return clientDetailsService;
        }


        class ClientTokenEnhancer implements TokenEnhancer {

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


                final Map<String, Object> additionalInfo = Map.of(ExtraClaims.CLIENT_ID, client.getId());
                ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);

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
                    .resourceId(OAuthSettings.RESOURCE_ID);
            //.stateless(false);
        }

        /**
         * For more supported expression based access control, see: OAuth2SecurityExpressionMethods
         *
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
                    .antMatchers("/actuator/**", "/clients/default").permitAll()
                    .antMatchers(HttpMethod.DELETE, "/clients/**").access("hasAuthority('MASTER')")
                    .antMatchers(HttpMethod.POST, "/clients").permitAll()
                    .antMatchers(HttpMethod.POST, "/clients/me/users").access("hasAuthority('ADMIN') and #oauth2.hasScopeMatching('client:write')")
                    .antMatchers(HttpMethod.GET, "/products/**", "/productoptions/**").access("hasAnyAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
                    .antMatchers(HttpMethod.POST, "/products/**", "/productoptions/**").access("hasAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
                    .anyRequest().authenticated();
        }
    }

    public static class OAuthSettings {

        public static final String RESOURCE_ID = "nextpos-service";

        private static final String SIGNING_KEY = "1qaz2wsx";
    }

    public static class OAuthScopes {

        public static final List<String> SCOPES = Arrays.asList(
                "client:read",
                "client:write",
                "user:read",
                "user:write",
                "product:read",
                "product:write",
                "all"
        );
    }
}
