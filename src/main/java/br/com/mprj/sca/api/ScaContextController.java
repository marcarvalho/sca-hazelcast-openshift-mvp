package br.com.mprj.sca.api;

import br.com.mprj.sca.context.ScaContext;
import br.com.mprj.sca.context.ScaContextRequest;
import br.com.mprj.sca.context.ScaContextService;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sca-context")
public class ScaContextController {

    private final ScaContextService service;
    private final HazelcastInstance hazelcastInstance;

    public ScaContextController(ScaContextService service, HazelcastInstance hazelcastInstance) {
        this.service = service;
        this.hazelcastInstance = hazelcastInstance;
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> save(@PathVariable String sessionId,
                                                    @RequestBody(required = false) ScaContextRequest request) throws Exception {
        ScaContextRequest safeRequest = request == null
                ? new ScaContextRequest("usuario-demo", "access-token-ref-demo", "refresh-token-ref-demo", Map.of())
                : request;

        Map<String, Object> attributes = new LinkedHashMap<>();
        if (safeRequest.attributes() != null) {
            attributes.putAll(safeRequest.attributes());
        }
        attributes.put("savedByPod", podName());

        ScaContext context = new ScaContext(
                sessionId,
                safeRequest.username(),
                safeRequest.accessTokenReference(),
                safeRequest.refreshTokenReference(),
                attributes
        );
        service.save(sessionId, context);

        return ResponseEntity.ok(response("saved", true, "sessionId", sessionId, "context", context));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> find(@PathVariable String sessionId) throws Exception {
        return ResponseEntity.ok(response(
                "sessionId", sessionId,
                "found", service.find(sessionId).orElse(null)
        ));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String sessionId,
                                                      @RequestBody ScaContextRequest request) throws Exception {
        ScaContext context = new ScaContext(
                sessionId,
                request.username(),
                request.accessTokenReference(),
                request.refreshTokenReference(),
                request.attributes()
        );
        return ResponseEntity.ok(response("updated", true, "context", service.update(sessionId, context)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String sessionId) throws Exception {
        return ResponseEntity.ok(response("removed", service.remove(sessionId), "sessionId", sessionId));
    }

    private Map<String, Object> response(Object... keyValues) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        map.put("pod", podName());
        map.put("mapName", service.mapName());
        map.put("mapSize", service.size());
        map.put("clusterName", hazelcastInstance.getConfig().getClusterName());
        map.put("clusterMembers", hazelcastInstance.getCluster().getMembers().size());
        map.put("members", hazelcastInstance.getCluster().getMembers().stream()
                .map(member -> member.getAddress().toString())
                .toList());
        return map;
    }

    private String podName() throws Exception {
        return InetAddress.getLocalHost().getHostName();
    }
}
