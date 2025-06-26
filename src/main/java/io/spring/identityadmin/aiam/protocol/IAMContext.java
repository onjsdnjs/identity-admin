package io.spring.identityadmin.aiam.protocol;

import io.spring.aicore.protocol.DomainContext;
import io.spring.identityadmin.aiam.protocol.enums.AuditRequirement;
import io.spring.identityadmin.aiam.protocol.enums.SecurityLevel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IAM 도메인 컨텍스트 기본 클래스
 * 모든 IAM 관련 AI 작업에 필요한 공통 컨텍스트 정보를 제공
 */
public abstract class IAMContext extends DomainContext {
    
    private final SecurityLevel securityLevel;
    private final AuditRequirement auditRequirement;
    private final Map<String, Object> iamMetadata;
    
    private String organizationId;
    private String tenantId;
    private List<String> userRoles;
    private List<String> userPermissions;
    private SecurityContext securityContext;
    
    protected IAMContext(SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super();
        this.securityLevel = securityLevel;
        this.auditRequirement = auditRequirement;
        this.iamMetadata = new ConcurrentHashMap<>();
    }
    
    protected IAMContext(String userId, String sessionId, SecurityLevel securityLevel, AuditRequirement auditRequirement) {
        super(userId, sessionId);
        this.securityLevel = securityLevel;
        this.auditRequirement = auditRequirement;
        this.iamMetadata = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getDomainType() {
        return "IAM";
    }
    
    @Override
    public int getPriorityLevel() {
        return securityLevel.getLevel();
    }
    
    /**
     * IAM 컨텍스트의 특화 타입을 반환합니다
     * @return IAM 컨텍스트 타입 (예: "POLICY", "RISK", "USER", "ROLE")
     */
    public abstract String getIAMContextType();
    
    /**
     * 이 컨텍스트가 감사 로깅을 필요로 하는지 확인합니다
     * @return 감사 로깅 필요 여부
     */
    public boolean requiresAuditLogging() {
        return auditRequirement != AuditRequirement.NONE;
    }
    
    /**
     * 보안 승인이 필요한지 확인합니다
     * @return 보안 승인 필요 여부
     */
    public boolean requiresSecurityApproval() {
        return securityLevel == SecurityLevel.HIGH || securityLevel == SecurityLevel.CRITICAL;
    }
    
    /**
     * IAM 메타데이터를 추가합니다
     * @param key 키
     * @param value 값
     */
    public void addIAMMetadata(String key, Object value) {
        this.iamMetadata.put(key, value);
    }
    
    /**
     * IAM 메타데이터를 조회합니다
     * @param key 키
     * @param type 값의 타입
     * @return 값 (존재하지 않으면 null)
     */
    @SuppressWarnings("unchecked")
    public <T> T getIAMMetadata(String key, Class<T> type) {
        Object value = iamMetadata.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    // Getters and Setters
    public SecurityLevel getSecurityLevel() { return securityLevel; }
    public AuditRequirement getAuditRequirement() { return auditRequirement; }
    public Map<String, Object> getAllIAMMetadata() { return Map.copyOf(iamMetadata); }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public List<String> getUserRoles() { return userRoles; }
    public void setUserRoles(List<String> userRoles) { this.userRoles = userRoles; }
    
    public List<String> getUserPermissions() { return userPermissions; }
    public void setUserPermissions(List<String> userPermissions) { this.userPermissions = userPermissions; }
    
    public SecurityContext getSecurityContext() { return securityContext; }
    public void setSecurityContext(SecurityContext securityContext) { this.securityContext = securityContext; }
    
    // ✅ 내부 클래스 분리 완료 - 별도 파일로 이동됨
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', iamType='%s', security=%s, audit=%s}", 
                getClass().getSimpleName(), getContextId(), getIAMContextType(), 
                securityLevel, auditRequirement);
    }
}
