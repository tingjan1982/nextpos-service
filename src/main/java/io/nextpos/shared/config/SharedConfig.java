package io.nextpos.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;

import javax.sql.DataSource;

/**
 * This is in place to resolve a circular dependency issue where
 * ClientService depends on these beans in SecurityConfig and in turn depends on
 * ClientService indirectly through defined interceptors.
 */
@Configuration
public class SharedConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JdbcClientDetailsService jdbcClientDetailsService(DataSource dataSource, PasswordEncoder passwordEncoder) {
        final JdbcClientDetailsService clientDetailsService = new JdbcClientDetailsService(dataSource);
        clientDetailsService.setPasswordEncoder(passwordEncoder);

        return clientDetailsService;
    }
}
