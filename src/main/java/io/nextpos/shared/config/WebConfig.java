package io.nextpos.shared.config;

import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    private final ClientResolver clientResolver;

    @Autowired
    public WebConfig(final ClientResolver clientResolver) {
        this.clientResolver = clientResolver;
    }

    @Bean
    public FilterRegistrationBean<ClientResolver> filterRegistrationBean() {

        FilterRegistrationBean<ClientResolver> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(clientResolver);
        registrationBean.addUrlPatterns("/products/*");

        return registrationBean;
    }
}
