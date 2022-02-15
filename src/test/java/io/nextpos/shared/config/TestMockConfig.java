package io.nextpos.shared.config;

import com.mongodb.BasicDBList;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.runtime.Network;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.auth.AuthenticationHelper;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mongodb.MongoCollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;

/**
 * Start MongoDb in replica for transaction support. Part of the code is referenced from MongoAutoConfiguration.
 * <p>
 * https://github.com/spring-projects/spring-boot/issues/20182
 * https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/257
 * https://www.programcreek.com/java-api-examples/?api=de.flapdoodle.embed.mongo.config.Storage
 */
@Configuration
@ConditionalOnProperty(value = "script", havingValue = "false", matchIfMissing = true)
public class TestMockConfig {

    private static final byte[] IP4_LOOPBACK_ADDRESS = {127, 0, 0, 1};

    private static final byte[] IP6_LOOPBACK_ADDRESS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private final MongoProperties properties;

    @Autowired
    public TestMockConfig(final MongoProperties properties) {
        this.properties = properties;
    }

    /**
     * Override the configuration in EmbeddedMongoAutoConfiguration to start Mongo server with replica set and journal.
     */
    @Bean
    public MongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {

        MongoCmdOptions cmdOptions = ImmutableMongoCmdOptions.builder()
                .useNoJournal(false)
                .build();

        ImmutableMongodConfig.Builder builder = MongodConfig.builder().version(determineVersion(embeddedProperties)).cmdOptions(cmdOptions);
        builder.stopTimeoutInMillis(6000);

        EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
        if (storage != null) {
            String databaseDir = storage.getDatabaseDir();
            String replSetName = storage.getReplSetName();
            int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
            builder.replication(new Storage(databaseDir, replSetName, oplogSize));
        }
        Integer configuredPort = this.properties.getPort();
        if (configuredPort != null && configuredPort > 0) {
            builder.net(new Net(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6()));
        }
        else {
            builder.net(new Net(getHost().getHostAddress(), Network.getFreeServerPort(getHost()),
                    Network.localhostIsIPv6()));
        }
        return builder.build();
    }

    private IFeatureAwareVersion determineVersion(EmbeddedMongoProperties embeddedProperties) {
        if (embeddedProperties.getFeatures() == null) {
            for (Version version : Version.values()) {
                if (version.asInDownloadPath().equals(embeddedProperties.getVersion())) {
                    return version;
                }
            }
            return Versions.withFeatures(createEmbeddedMongoVersion(embeddedProperties));
        }
        return Versions.withFeatures(createEmbeddedMongoVersion(embeddedProperties),
                embeddedProperties.getFeatures().toArray(new Feature[0]));
    }

    private de.flapdoodle.embed.process.distribution.Version.GenericVersion createEmbeddedMongoVersion(EmbeddedMongoProperties embeddedProperties) {
        return de.flapdoodle.embed.process.distribution.Version.of(embeddedProperties.getVersion());
    }

    private InetAddress getHost() throws UnknownHostException {
        if (this.properties.getHost() == null) {
            return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
        }
        return InetAddress.getByName(this.properties.getHost());
    }

    @Bean
    public MongoInitializer mongoInitializer(MongoClient mongoClient, MongoTemplate mongoTemplate) {
        return new MongoInitializer(mongoClient, mongoTemplate);
    }

    static class MongoInitializer {

        private final MongoClient mongoClient;

        private final MongoTemplate template;

        MongoInitializer(MongoClient mongoClient, MongoTemplate template) {
            this.mongoClient = mongoClient;
            this.template = template;
        }

        /**
         * Create collections dynamically.
         */
        @PostConstruct
        public void afterPropertiesSet() throws Exception {

            final ServerAddress address = mongoClient.getClusterDescription().getServerDescriptions().get(0).getAddress();
            BasicDBList members = new BasicDBList();
            members.add(new Document("_id", 0).append("host", address.getHost() + ":" + address.getPort()));
            Document config = new Document("_id", "rep0");
            config.put("members", members);
            MongoDatabase admin = mongoClient.getDatabase("admin");
            admin.runCommand(new Document("replSetInitiate", config));

            // this allows time for replica set to finish initiation.
            TimeUnit.SECONDS.sleep(2);

            Awaitility.await().atMost(Duration.TWO_MINUTES).until(() -> {
                try (ClientSession session = mongoClient.startSession()) {
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            });

            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.data.mongodb.core.mapping.Document.class));

            scanner.findCandidateComponents("io.nextpos").forEach(b -> {
                try {
                    final String collection = MongoCollectionUtils.getPreferredCollectionName(Class.forName(b.getBeanClassName()));

                    if (!template.collectionExists(collection)) {
                        template.createCollection(collection);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    @Bean
    @ConditionalOnProperty(name = "nomock", havingValue = "false", matchIfMissing = true)
    public AuthenticationHelper authenticationHelper() {
        final AuthenticationHelper mock = Mockito.mock(AuthenticationHelper.class);
        final Client client = DummyObjects.dummyClient();
        final ClientUser clientUser = DummyObjects.dummyClientUser(client);

        Mockito.when(mock.resolveCurrentUsername()).thenReturn(clientUser.getUsername());
        Mockito.when(mock.resolveCurrentClientId()).thenReturn(client.getUsername());

        return mock;
    }

    @Bean
    public OrderSettings defaultOrderSettings(CountrySettings defaultCountrySettings) {
        return new OrderSettings(defaultCountrySettings.getTaxRate(),
                false,
                defaultCountrySettings.getCurrency(),
                BigDecimal.valueOf(0.1),
                2,
                RoundingMode.HALF_UP);
    }
}
