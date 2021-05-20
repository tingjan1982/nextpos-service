package io.nextpos.storage.config;

import org.springframework.context.annotation.Configuration;

/**
 * Reference
 *
 * Spring Boot with Hazelcast:
 * https://medium.com/@ihorkosandiak/spring-boot-with-hazelcast-b04d13927745
 *
 * Hazelcast Client:
 * https://www.baeldung.com/java-hazelcast
 *
 * Hazelcast sidecar pattern:
 * https://hazelcast.com/blog/hazelcast-sidecar-container-pattern/
 */
@Deprecated
@Configuration
public class HazelcastConfig {

//    @Bean
//    public Config myHazelcastConfig() {
//        Config config = new Config();
//        config.setInstanceName("hazelcast-instance")
//                .addMapConfig(new MapConfig()
//                        .setName("configuration")
//                        .setMaxSizeConfig(new MaxSizeConfig(200, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
//                        .setEvictionPolicy(EvictionPolicy.LRU)
//                        .setTimeToLiveSeconds(0));
//
//        return config;
//    }
}
