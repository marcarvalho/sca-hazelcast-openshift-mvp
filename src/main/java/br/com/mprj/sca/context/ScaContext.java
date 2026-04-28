package br.com.mprj.sca.context;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ScaContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String username;
    private String accessTokenReference;
    private String refreshTokenReference;
    private Map<String, Object> attributes = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;

    public ScaContext() {
    }

    public ScaContext(String sessionId,
                      String username,
                      String accessTokenReference,
                      String refreshTokenReference,
                      Map<String, Object> attributes) {
        this.sessionId = sessionId;
        this.username = username;
        this.accessTokenReference = accessTokenReference;
        this.refreshTokenReference = refreshTokenReference;
        this.attributes = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAccessTokenReference() { return accessTokenReference; }
    public void setAccessTokenReference(String accessTokenReference) { this.accessTokenReference = accessTokenReference; }
    public String getRefreshTokenReference() { return refreshTokenReference; }
    public void setRefreshTokenReference(String refreshTokenReference) { this.refreshTokenReference = refreshTokenReference; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
