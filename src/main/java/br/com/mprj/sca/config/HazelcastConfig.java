package br.com.mprj.sca.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HazelcastProperties.class)
public class HazelcastConfig {

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance(HazelcastProperties properties) {
        Config config = new Config();
        config.setClusterName(properties.getClusterName());
        config.setInstanceName(properties.getInstanceName());

        NetworkConfig network = config.getNetworkConfig();
        network.setPort(properties.getPort());
        network.setPortAutoIncrement(true);

        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        if (properties.getKubernetes().isEnabled()) {
            String serviceDns = properties.getKubernetes().getServiceName() + "." + properties.getKubernetes().getNamespace() + ".svc.cluster.local";

            join.getMulticastConfig().setEnabled(false);
            join.getTcpIpConfig().setEnabled(false);
            join.getAutoDetectionConfig().setEnabled(false);
            join.getKubernetesConfig()
                    .setEnabled(true)
                    .setProperty("service-dns", serviceDns)
                    .setProperty("service-port", Integer.toString(properties.getPort()))
                    .setProperty("resolve-not-ready-addresses", "true");
        } else {
            join.getTcpIpConfig()
                    .setEnabled(true)
                    .addMember("127.0.0.1");
        }

        MapConfig scaContextMap = new MapConfig(properties.getMapName())
                .setBackupCount(properties.getBackupCount())
                .setAsyncBackupCount(0)
                .setTimeToLiveSeconds(properties.getTtlSeconds())
                .setMaxIdleSeconds(properties.getMaxIdleSeconds())
                .setEvictionConfig(new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.LRU)
                        .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                        .setSize(properties.getMaxSizePerNode()));

        config.addMapConfig(scaContextMap);

        return Hazelcast.newHazelcastInstance(config);
    }
}
