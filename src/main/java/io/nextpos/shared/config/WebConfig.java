package io.nextpos.shared.config;

import io.nextpos.shared.web.ClientResolver;
import io.nextpos.shared.web.ClientUsageTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ClientResolver clientResolver;

    private final ClientUsageTracker clientUsageTracker;

    @Autowired
    public WebConfig(final ClientResolver clientResolver, ClientUsageTracker clientUsageTracker) {
        this.clientResolver = clientResolver;
        this.clientUsageTracker = clientUsageTracker;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(clientUsageTracker);
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
                "/clients/me/*",
                "/clientstatus/*",
                "/orders/*",
                "/ordersets/*",
                "/splitOrders/*",
                "/splitOrders/headcount/*",
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
                "/einvoices/*",
                "/invoiceNumbers/*",
                "/clientSubscriptions/*",
                "/memberships/*",
                "/rosterPlans/*",
                "/linkedClientAccounts/*");

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
