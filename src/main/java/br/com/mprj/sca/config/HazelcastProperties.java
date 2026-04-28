package br.com.mprj.sca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hazelcast")
public class HazelcastProperties {

    private String clusterName = "sca-cluster";
    private String instanceName = "sca-hazelcast-member";
    private String mapName = "scaContext";
    private int port = 5701;
    private int ttlSeconds = 1800;
    private int maxIdleSeconds = 900;
    private int backupCount = 1;
    private int maxSizePerNode = 10000;
    private final Kubernetes kubernetes = new Kubernetes();

    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    public int getMaxIdleSeconds() { return maxIdleSeconds; }
    public void setMaxIdleSeconds(int maxIdleSeconds) { this.maxIdleSeconds = maxIdleSeconds; }
    public int getBackupCount() { return backupCount; }
    public void setBackupCount(int backupCount) { this.backupCount = backupCount; }
    public int getMaxSizePerNode() { return maxSizePerNode; }
    public void setMaxSizePerNode(int maxSizePerNode) { this.maxSizePerNode = maxSizePerNode; }
    public Kubernetes getKubernetes() { return kubernetes; }

    public static class Kubernetes {
        private boolean enabled;
        private String serviceName = "sca-service-hazelcast";
        private String namespace = "default";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
    }
}
