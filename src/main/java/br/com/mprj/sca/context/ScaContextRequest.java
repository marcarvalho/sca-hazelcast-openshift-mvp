package br.com.mprj.sca.context;

import java.util.Map;

public record ScaContextRequest(
        String username,
        String accessTokenReference,
        String refreshTokenReference,
        Map<String, Object> attributes
) {
}
