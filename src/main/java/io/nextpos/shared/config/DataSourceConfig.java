package io.nextpos.shared.config;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.product.data.Product;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.converter.Decimal128ToBigDecimal;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.workingarea.data.WorkingArea;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.List;

/**
 * Declare separate @Enable annotation to enable mongodb and jpa data support:
 * https://stackoverflow.com/questions/25529283/unable-to-use-spring-data-mongodb-spring-datajpa-together-with-springboot
 *
 *  Override the default MappingMongoConverter in MongoDataAutoConfiguration in order to
 *  register a custom Converter that will later be used to convert Mongo Decimal128 type to BigDecimal type that is used
 *  in Order Mongo Document.
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = {Client.class, Product.class, Offer.class, CountrySettings.class, WorkingArea.class, TableLayout.class})
@EnableMongoRepositories(basePackageClasses = {Order.class, OrderTransaction.class, NotificationDetails.class, UserTimeCard.class, Membership.class, Announcement.class, ElectronicInvoice.class})
@EnableMongoAuditing
public class DataSourceConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new Decimal128ToBigDecimal()));
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory, MongoMappingContext context, MongoCustomConversions conversions) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
        mappingConverter.setCustomConversions(conversions);

        return mappingConverter;
    }
}
