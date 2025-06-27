package io.spring.iam.aiam.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 보안 컨텍스트 정보 클래스
 * ✅ SRP 준수: 보안 컨텍스트 관리만 담당
 */
public class SecurityContext {
    private final Map<String, Object> securityAttributes;
    private String currentUser;
    private String sessionId;
    private String sourceIp;
    private boolean authenticated;
    
    public SecurityContext() {
        this.securityAttributes = new ConcurrentHashMap<>();
        this.authenticated = false;
    }
    
    public SecurityContext(String currentUser, String sessionId) {
        this();
        this.currentUser = currentUser;
        this.sessionId = sessionId;
        this.authenticated = true;
    }
    
    public void addSecurityAttribute(String key, Object value) {
        this.securityAttributes.put(key, value);
    }
    
    public Object getSecurityAttribute(String key) {
        return this.securityAttributes.get(key);
    }
    
    // Getters and Setters
    public Map<String, Object> getSecurityAttributes() { return Map.copyOf(securityAttributes); }
    public String getCurrentUser() { return currentUser; }
    public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    
    @Override
    public String toString() {
        return String.format("SecurityContext{user='%s', session='%s', authenticated=%s}", 
                currentUser, sessionId, authenticated);
    }
} 