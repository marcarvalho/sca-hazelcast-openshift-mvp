package br.com.mprj.sca.api;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final HazelcastInstance hazelcastInstance;

    public ClusterController(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @GetMapping
    public Map<String, Object> cluster() throws Exception {
        return Map.of(
                "pod", InetAddress.getLocalHost().getHostName(),
                "clusterName", hazelcastInstance.getConfig().getClusterName(),
                "membersCount", hazelcastInstance.getCluster().getMembers().size(),
                "members", hazelcastInstance.getCluster().getMembers().stream()
                        .map(member -> member.getAddress().toString())
                        .toList()
        );
    }
}
