package br.com.mprj.sca.context;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ScaContextService {

    private final IMap<String, ScaContext> scaContextMap;
    private final int ttlSeconds;

    public ScaContextService(HazelcastInstance hazelcastInstance,
                             @Value("${hazelcast.map-name:scaContext}") String mapName,
                             @Value("${hazelcast.ttl-seconds:1800}") int ttlSeconds) {
        this.scaContextMap = hazelcastInstance.getMap(mapName);
        this.ttlSeconds = ttlSeconds;
    }

    public void save(String key, ScaContext context) {
        scaContextMap.set(key, context, ttlSeconds, TimeUnit.SECONDS);
    }

    public Optional<ScaContext> find(String key) {
        return Optional.ofNullable(scaContextMap.get(key));
    }

    public ScaContext update(String key, ScaContext context) {
        context.touch();
        scaContextMap.set(key, context, ttlSeconds, TimeUnit.SECONDS);
        return context;
    }

    public boolean remove(String key) {
        return scaContextMap.remove(key) != null;
    }

    public int size() {
        return scaContextMap.size();
    }

    public String mapName() {
        return scaContextMap.getName();
    }
}
