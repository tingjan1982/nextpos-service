package io.nextpos.shared.config;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.auth.OAuth2Helper;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mongodb.MongoCollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.mockito.ArgumentMatchers.any;

@Configuration
public class TestMockConfig {

    private static final byte[] IP4_LOOPBACK_ADDRESS = {127, 0, 0, 1};

    private static final byte[] IP6_LOOPBACK_ADDRESS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private final MongoProperties properties;

    public TestMockConfig(final MongoProperties properties) {
        this.properties = properties;
    }

    /**
     * Override the configuration in EmbeddedMongoAutoConfiguration to start Mongo server with replica set and journal.
     */
    @Bean
    public IMongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
        IMongoCmdOptions cmdOptions = new MongoCmdOptionsBuilder()
                .useNoJournal(false)
                .build();

        MongodConfigBuilder builder = new MongodConfigBuilder().version(determineVersion(embeddedProperties)).cmdOptions(cmdOptions);

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
        } else {
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
            return Versions.withFeatures(new GenericVersion(embeddedProperties.getVersion()));
        }
        return Versions.withFeatures(new GenericVersion(embeddedProperties.getVersion()),
                embeddedProperties.getFeatures().toArray(new Feature[0]));
    }

    private InetAddress getHost() throws UnknownHostException {
        if (this.properties.getHost() == null) {
            return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
        }
        return InetAddress.getByName(this.properties.getHost());
    }

    /**
     * Start MongoDb in replica for transaction support. Part of the code is referenced from MongoAutoConfiguration.
     * <p>
     * https://github.com/spring-projects/spring-boot/issues/20182
     * https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/257
     * https://www.programcreek.com/java-api-examples/?api=de.flapdoodle.embed.mongo.config.Storage
     */
    @Bean
    public MongoClient mongo(MongoProperties properties, ObjectProvider<MongoClientOptions> options, Environment environment) {

        //final MongoClient client = new MongoClientFactory(properties, environment).createMongoClient(options.getIfAvailable());
        MongoClientOptions mongoClientOptions = options.getIfAvailable();

        if (mongoClientOptions == null) {
            mongoClientOptions = MongoClientOptions.builder().build();
        }

        int port = 0;

        if (environment != null) {
            String localPort = environment.getProperty("local.mongo.port");
            if (localPort != null) {
                port = Integer.parseInt(localPort);
            }
        }

        String host = (this.properties.getHost() != null) ? this.properties.getHost() : "localhost";
        MongoClient client = new MongoClient(new ServerAddress(host, port), mongoClientOptions);

        ServerAddress address = client.getAllAddress().get(0);
        BasicDBList members = new BasicDBList();
        members.add(new Document("_id", 0).append("host", address.getHost() + ":" + address.getPort()));
        Document config = new Document("_id", "rep0");
        config.put("members", members);
        MongoDatabase admin = client.getDatabase("admin");
        admin.runCommand(new Document("replSetInitiate", config));

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> {
            try (ClientSession session = client.startSession()) {
                return true;
            } catch (Exception ex) {
                return false;
            }
        });


        return client;
    }

//    @Bean
//    public MongoClientOptions mongoClientOptions() {
//        return MongoClientOptions.builder()
//                .requiredReplicaSetName("rep0").build();
//    }

    @Bean
    public MongoInitializer mongoInitializer(MongoTemplate mongoTemplate) {
        return new MongoInitializer(mongoTemplate);
    }

    static class MongoInitializer implements InitializingBean {

        private final MongoTemplate template;

        MongoInitializer(MongoTemplate template) {
            this.template = template;
        }

        /**
         * Create collections dynamically.
         */
        @Override
        public void afterPropertiesSet() {

            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.data.mongodb.core.mapping.Document.class));

            scanner.findCandidateComponents("io.nextpos").forEach(b -> {
                try {
                    final String collection = MongoCollectionUtils.getPreferredCollectionName(Class.forName(b.getBeanClassName()));

                    // todo: need to figure out why membership is created beforehand.
                    if (!collection.equals("membership")) {
                        this.template.createCollection(collection);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    @Bean
    @ConditionalOnProperty(name = "nomock", havingValue = "false", matchIfMissing = true)
    public OAuth2Helper oAuth2Helper() {
        final OAuth2Helper mock = Mockito.mock(OAuth2Helper.class);
        final ClientUser clientUser = DummyObjects.dummyClientUser();

        Mockito.when(mock.getCurrentPrincipal()).thenReturn(clientUser.getId().getUsername());
        Mockito.when(mock.resolveCurrentClientUser(any(Client.class))).thenReturn(clientUser);

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
