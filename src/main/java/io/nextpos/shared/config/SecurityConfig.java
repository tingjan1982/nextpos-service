package io.nextpos.shared.config;

import io.nextpos.client.service.ClientServiceImpl;
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
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * https://www.baeldung.com/spring-boot-security-autoconfiguration
 * <p>
 * https://www.baeldung.com/spring-security-oauth-jwt
 * <p>
 * Contains core security configuration and shared beans that are needed by the authorization server and resource server.
 *
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


    /**
     * Authorization server configuration
     */
    @EnableAuthorizationServer
    public static class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

        private final TokenStore tokenStore;

        private final AccessTokenConverter accessTokenConverter;

        private final AuthenticationManager authenticationManager;

        private final JdbcClientDetailsService clientDetailsService;

        @Autowired
        public AuthorizationServer(final DataSource dataSource, final TokenStore tokenStore, final AccessTokenConverter accessTokenConverter, final AuthenticationManager authenticationManager, final PasswordEncoder passwordEncoder) {
            this.tokenStore = tokenStore;
            this.accessTokenConverter = accessTokenConverter;
            this.authenticationManager = authenticationManager;

            clientDetailsService = new JdbcClientDetailsService(dataSource);
            clientDetailsService.setPasswordEncoder(passwordEncoder);
        }


        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.tokenStore(tokenStore)
                    .accessTokenConverter(accessTokenConverter)
                    .authenticationManager(authenticationManager);
        }

        @Override
        public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(jdbcClientDetailsService());
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
    }


    @EnableResourceServer
    public static class ResourceServer extends ResourceServerConfigurerAdapter {

        private final ResourceServerTokenServices tokenServices;

        @Autowired
        public ResourceServer(final ResourceServerTokenServices tokenService) {
            this.tokenServices = tokenService;
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
         * @param http
         * @throws Exception
         */
        @Override
        public void configure(HttpSecurity http) throws Exception {

            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            http.csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/actuator/**", "/clients/default").permitAll()
                    .antMatchers(HttpMethod.DELETE, "/clients/**").access("hasAuthority('MASTER')")
                    .antMatchers(HttpMethod.POST, "/clients").permitAll()
                    .antMatchers(HttpMethod.POST, "/clients/me/users").access("hasAuthority('ADMIN') and #oauth2.hasScopeMatching('client:write')")
                    .antMatchers(HttpMethod.GET,"/products/**", "/productoptions/**").access("hasAnyAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
                    .antMatchers(HttpMethod.POST,"/products/**", "/productoptions/**").access("hasAuthority('USER') and #oauth2.hasScopeMatching('product:.*')")
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
