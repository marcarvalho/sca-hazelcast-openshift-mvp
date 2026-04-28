package br.com.mprj.sca.context;

import br.com.mprj.sca.ScaHazelcastOpenShiftApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ScaHazelcastOpenShiftApplication.class, properties = {
        "hazelcast.kubernetes.enabled=false",
        "hazelcast.cluster-name=test-sca-cluster",
        "hazelcast.map-name=scaContextTest"
})
class ScaContextServiceTest {

    @Autowired
    private ScaContextService service;

    @Test
    void shouldSaveAndFindScaContext() {
        ScaContext context = new ScaContext(
                "SID-001",
                "marcao",
                "access-ref",
                "refresh-ref",
                Map.of("origin", "test")
        );

        service.save("SID-001", context);

        assertThat(service.find("SID-001")).isPresent();
        assertThat(service.find("SID-001").orElseThrow().getUsername()).isEqualTo("marcao");
    }
}
