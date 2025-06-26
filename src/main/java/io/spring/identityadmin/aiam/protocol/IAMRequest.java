package io.spring.identityadmin.aiam.protocol;

import io.spring.aicore.protocol.AIRequest;
import io.spring.identityadmin.aiam.protocol.enums.AuditRequirement;
import io.spring.identityadmin.aiam.protocol.enums.SecurityLevel;
import org.springframework.security.core.context.SecurityContext;

/**
 * IAM AI 요청 클래스
 * AI Core 요청을 확장하여 IAM 특화 기능을 제공
 * 
 * @param <T> IAM 컨텍스트 타입
 */
public class IAMRequest<T extends IAMContext> extends AIRequest<T> {
    
    private String organizationId;
    private String tenantId;
    private boolean requiresApproval;
    private AuditRequirement auditLevel;
    
    public IAMRequest(T context, String operation) {
        super(context, operation);
        this.auditLevel = context.getAuditRequirement();
        this.requiresApproval = context.requiresSecurityApproval();
    }
    
    public IAMRequest(T context, String operation, RequestPriority priority, RequestType requestType) {
        super(context, operation, priority, requestType);
        this.auditLevel = context.getAuditRequirement();
        this.requiresApproval = context.requiresSecurityApproval();
    }
    
    /**
     * 조직 ID를 설정합니다
     * @param organizationId 조직 ID
     * @return 체이닝을 위한 현재 객체
     */
    public IAMRequest<T> withOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    /**
     * 테넌트 ID를 설정합니다
     * @param tenantId 테넌트 ID
     * @return 체이닝을 위한 현재 객체
     */
    public IAMRequest<T> withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    /**
     * 승인 요구사항을 설정합니다
     * @param requiresApproval 승인 필요 여부
     * @return 체이닝을 위한 현재 객체
     */
    public IAMRequest<T> withApprovalRequirement(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
        return this;
    }
    
    /**
     * 감사 레벨을 설정합니다
     * @param auditLevel 감사 레벨
     * @return 체이닝을 위한 현재 객체
     */
    public IAMRequest<T> withAuditLevel(AuditRequirement auditLevel) {
        this.auditLevel = auditLevel;
        return this;
    }
    
    /**
     * 이 요청이 높은 권한을 요구하는지 확인합니다
     * @return 높은 권한 요구 여부
     */
    public boolean isHighPrivilegeRequest() {
        return getContext().getSecurityLevel() == SecurityLevel.ENHANCED || 
               getContext().getSecurityLevel() == SecurityLevel.MAXIMUM;
    }
    
    /**
     * 이 요청이 실시간 감사를 요구하는지 확인합니다
     * @return 실시간 감사 요구 여부
     */
    public boolean requiresRealTimeAudit() {
        return auditLevel == AuditRequirement.DETAILED || 
               auditLevel == AuditRequirement.COMPREHENSIVE;
    }
    
    /**
     * 보안 컨텍스트를 요청에 추가합니다
     * @param securityContext Spring Security 컨텍스트
     * @return 체이닝을 위한 현재 객체
     */
    public IAMRequest<T> addSecurityContext(SecurityContext securityContext) {
        // 보안 컨텍스트를 메타데이터로 저장
        withParameter("securityContext", securityContext);
        withParameter("principal", securityContext.getAuthentication().getName());
        withParameter("authorities", securityContext.getAuthentication().getAuthorities());
        return this;
    }
    
    // Getters
    public String getOrganizationId() { return organizationId; }
    public String getTenantId() { return tenantId; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public AuditRequirement getAuditLevel() { return auditLevel; }
    
    @Override
    public String toString() {
        return String.format("IAMRequest{id='%s', operation='%s', iamType='%s', security=%s, audit=%s}", 
                getRequestId(), getOperation(), getContext().getIAMContextType(),
                getContext().getSecurityLevel(), auditLevel);
    }
} 