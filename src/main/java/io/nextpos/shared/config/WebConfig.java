package io.nextpos.shared.config;

import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

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
        registrationBean.addUrlPatterns(
                "/products/*",
                "/productoptions/*",
                "/labels/*",
                "/clients/me",
                "/clients/me/settings/*",
                "/clients/me/users/*",
                "/clientstatus/*",
                "/orders/*",
                "/shifts/*",
                "/timecards/*",
                "/reporting/*",
                "/searches/*",
                "/workingareas/*",
                "/printers/*",
                "/tablelayouts/*",
                "/announcements/*",
                "/roles/*",
                "/offers/*",
                "/invoiceNumbers/*");

        return registrationBean;
    }

    /**
     * API payload logging:
     * https://www.baeldung.com/spring-http-logging
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> commonsRequestLoggingFilter() {
        final CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1024 * 30);
        filter.setIncludeHeaders(false);

        FilterRegistrationBean<CommonsRequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");

        return registrationBean;
    }

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }
}
