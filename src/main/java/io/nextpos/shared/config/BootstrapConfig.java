package io.nextpos.shared.config;

import com.mongodb.client.MongoClient;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.subscription.data.SubscriptionPaymentInstruction;
import io.nextpos.subscription.service.SubscriptionPlanService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoCollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * https://www.baeldung.com/running-setup-logic-on-startup-in-spring
 */
@Component
@JpaTransaction
public class BootstrapConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfig.class);

    public static final String MASTER_CLIENT = "master-client";

    public static final String DEFAULT_COUNTRY_CODE = "TW";

    public static final String DEFAULT_TIME_ZONE = "Asia/Taipei";

    private final ClientService clientService;

    private final SettingsService settingsService;

    private final SubscriptionPlanService subscriptionPlanService;

    private final SettingsConfigurationProperties settingsProperties;

    private final MongoClient mongoClient;

    private final MongoTemplate mongoTemplate;

    private final DataSource dataSource;

    @Autowired
    public BootstrapConfig(final ClientService clientService, final SettingsService settingsService, SubscriptionPlanService subscriptionPlanService, final SettingsConfigurationProperties settingsProperties, MongoClient mongoClient, MongoTemplate mongoTemplate, final DataSource dataSource) {
        this.clientService = clientService;
        this.settingsService = settingsService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.settingsProperties = settingsProperties;
        this.mongoClient = mongoClient;
        this.mongoTemplate = mongoTemplate;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void bootstrap() throws Exception {

        createMongoTables();
        logJpaTables();

        Client defaultClient = clientService.getDefaultClient();

        if (defaultClient == null) {
            defaultClient = new Client(MASTER_CLIENT, MASTER_CLIENT, "1qaz2wsx3edc", DEFAULT_COUNTRY_CODE, DEFAULT_TIME_ZONE);
            defaultClient.setRoles(SecurityConfig.Role.MASTER_ROLE);
            final Client client = clientService.createClient(defaultClient);

            final String secret = new RandomValueStringGenerator(32).generate();
            LOGGER.info("test secret: {}", secret);
            LOGGER.info("Created master client: {}", client);
        }

        settingsService.findCountrySettings(DEFAULT_COUNTRY_CODE).ifPresentOrElse(settings -> {
            settings.setTaxInclusive(true);
            settings.setDecimalPlaces(0);
            settings.setRoundingMode(RoundingMode.HALF_UP);

            settings.getCommonAttributes().clear();
            settingsProperties.getCommonAttributes().forEach(settings::addCommonAttribute);
            settingsProperties.getCountryAttributes().getOrDefault(DEFAULT_COUNTRY_CODE, List.of()).forEach(settings::addCommonAttribute);

            LOGGER.info("Updating default country settings: {}", settings);
            settingsService.saveCountrySettings(settings);

        }, () -> {
            final CountrySettings defaultCountrySettings = new CountrySettings(DEFAULT_COUNTRY_CODE,
                    BigDecimal.valueOf(0.05),
                    true,
                    Currency.getInstance("TWD"),
                    0,
                    RoundingMode.HALF_UP);

            settingsProperties.getCommonAttributes().forEach(defaultCountrySettings::addCommonAttribute);
            settingsProperties.getCountryAttributes().getOrDefault(DEFAULT_COUNTRY_CODE, List.of()).forEach(defaultCountrySettings::addCommonAttribute);

            LOGGER.info("Creating default country settings: {}", defaultCountrySettings);
            settingsService.saveCountrySettings(defaultCountrySettings);
        });

        if (checkMongoDbSessionSupport()) {
            final SubscriptionPaymentInstruction paymentInstruction = subscriptionPlanService.getSubscriptionPaymentInstructionByCountry(DEFAULT_COUNTRY_CODE).orElseGet(() -> {

                final SubscriptionPaymentInstruction newInstruction = new SubscriptionPaymentInstruction(DEFAULT_COUNTRY_CODE, "d-dd8bd80c86c74ea9a9ff2a96dcfb462d");
                return subscriptionPlanService.saveSubscriptionPaymentInstruction(newInstruction);
            });

            LOGGER.info("Default subscription payment instruction: {}", paymentInstruction);
        }
    }

    private void createMongoTables() {

        if (!checkMongoDbSessionSupport()) {
            return;
        }

        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.data.mongodb.core.mapping.Document.class));

        scanner.findCandidateComponents("io.nextpos").forEach(b -> {
            try {
                final String collection = MongoCollectionUtils.getPreferredCollectionName(Class.forName(b.getBeanClassName()));

                if (!mongoTemplate.collectionExists(collection)) {
                    mongoTemplate.createCollection(collection);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Error while creating mongo tables: " + e.getMessage(), e);
            }
        });
    }

    private boolean checkMongoDbSessionSupport() {

        try {
            mongoClient.startSession();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void logJpaTables() throws MetaDataAccessException {
        LOGGER.debug("JPA tables");

        JdbcUtils.extractDatabaseMetaData(dataSource, metadata -> {
            try (ResultSet rs = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
                List<String> l = new ArrayList<>();
                while (rs.next()) {
                    final String tableName = rs.getString(3);
                    LOGGER.debug("Table: {}", tableName);
                    l.add(tableName);
                }
                return l;
            }
        });
    }

    /**
     * Index creation reference:
     * https://docs.spring.io/spring-data/data-mongodb/docs/current-SNAPSHOT/reference/html/#mapping.index-creation
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initIndicesAfterStartup() {
        LOGGER.info("Creating MongoDB indexes");

        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

        // consider only entities that are annotated with @Document
        mappingContext.getPersistentEntities()
                .stream()
                .filter(it -> it.isAnnotationPresent(Document.class))
                .forEach(it -> {
                    IndexOperations indexOps = mongoTemplate.indexOps(it.getType());
                    resolver.resolveIndexFor(it.getType()).forEach(indexOps::ensureIndex);
                });
    }

    @Bean
    @Lazy
    public Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers(Client defaultClient) {

        return Arrays.stream(OrderLevelOffer.GlobalOrderDiscount.values())
                .map(d -> {
                    final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(defaultClient,
                            d.getDiscountName(),
                            Offer.TriggerType.AT_CHECKOUT,
                            d.getDiscountType(),
                            d.getDiscount());
                    orderLevelOffer.setId(d.name());
                    orderLevelOffer.updateOfferEffectiveDetails(true);
                    return Pair.of(d, orderLevelOffer);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (existing, replacement) -> existing, LinkedHashMap::new));
    }

    @Bean
    @Lazy
    public Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers(Client defaultClient) {

        return Arrays.stream(ProductLevelOffer.GlobalProductDiscount.values())
                .map(d -> {
                    final ProductLevelOffer productLevelOffer = new ProductLevelOffer(defaultClient,
                            d.getDiscountName(),
                            Offer.TriggerType.AT_CHECKOUT,
                            d.getDiscountType(),
                            d.getDiscount(),
                            true);
                    productLevelOffer.setId(d.name());
                    productLevelOffer.updateOfferEffectiveDetails(true);
                    return Pair.of(d, productLevelOffer);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (existing, replacement) -> existing, LinkedHashMap::new));
    }

    @Bean
    @Lazy
    public Client defaultClient() {
        return clientService.getDefaultClient();
    }

    @Bean
    @Lazy
    public CountrySettings defaultCountrySettings() {
        return settingsService.getCountrySettings(DEFAULT_COUNTRY_CODE);
    }
}
