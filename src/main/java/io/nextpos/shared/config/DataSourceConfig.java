package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.product.data.Product;
import io.nextpos.settings.data.CountrySettings;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * https://stackoverflow.com/questions/25529283/unable-to-use-spring-data-mongodb-spring-datajpa-together-with-springboot
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = {Client.class, Product.class, CountrySettings.class})
@EnableMongoRepositories(basePackageClasses = {Order.class})
public class DataSourceConfig {

    // mongodb+srv://nextpos-dbadmin:<password>@nextpos-mongo-cluster-odlrm.gcp.mongodb.net/test?retryWrites=true&w=majority


}
