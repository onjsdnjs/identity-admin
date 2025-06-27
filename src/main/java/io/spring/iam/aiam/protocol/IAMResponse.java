package io.spring.iam.aiam.protocol;

import io.spring.aicore.protocol.AIResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IAM AI 응답 클래스
 * AI Core 응답을 확장하여 IAM 특화 정보를 제공
 */
public abstract class IAMResponse extends AIResponse {
    
    private final AuditInfo auditInfo;
    private final SecurityValidation securityValidation;
    private final Map<String, Object> iamSpecificMetadata;
    
    private String organizationId;
    private String tenantId;
    private boolean sensitiveDataIncluded;
    private ComplianceInfo complianceInfo;
    
    protected IAMResponse(String requestId, ExecutionStatus status) {
        super(requestId, status);
        this.auditInfo = new AuditInfo();
        this.securityValidation = new SecurityValidation();
        this.iamSpecificMetadata = new ConcurrentHashMap<>();
        this.sensitiveDataIncluded = false;
    }
    
    /**
     * IAM 응답의 특화 타입을 반환합니다
     * @return IAM 응답 타입 (예: "POLICY", "RISK_ASSESSMENT", "USER_ANALYSIS")
     */
    @Override
    public abstract String getResponseType();
    
    /**
     * IAM 메타데이터를 추가합니다
     * @param key 키
     * @param value 값
     * @return 체이닝을 위한 현재 객체
     */
    public IAMResponse withIAMMetadata(String key, Object value) {
        this.iamSpecificMetadata.put(key, value);
        return this;
    }
    
    /**
     * 조직 ID를 설정합니다
     * @param organizationId 조직 ID
     * @return 체이닝을 위한 현재 객체
     */
    public IAMResponse withOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    /**
     * 민감 데이터 포함 여부를 설정합니다
     * @param sensitiveDataIncluded 민감 데이터 포함 여부
     * @return 체이닝을 위한 현재 객체
     */
    public IAMResponse withSensitiveDataFlag(boolean sensitiveDataIncluded) {
        this.sensitiveDataIncluded = sensitiveDataIncluded;
        return this;
    }
    
    /**
     * 컴플라이언스 정보를 설정합니다
     * @param complianceInfo 컴플라이언스 정보
     * @return 체이닝을 위한 현재 객체
     */
    public IAMResponse withComplianceInfo(ComplianceInfo complianceInfo) {
        this.complianceInfo = complianceInfo;
        return this;
    }
    
    /**
     * 이 응답이 감사 로깅을 필요로 하는지 확인합니다
     * @return 감사 로깅 필요 여부
     */
    public boolean requiresAuditLogging() {
        return auditInfo.isAuditRequired() || sensitiveDataIncluded;
    }
    
    /**
     * IAM 메타데이터를 타입 안전하게 조회합니다
     * @param key 메타데이터 키
     * @param type 값의 타입
     * @return 메타데이터 값 (존재하지 않으면 null)
     */
    @SuppressWarnings("unchecked")
    public <T> T getIAMMetadata(String key, Class<T> type) {
        Object value = iamSpecificMetadata.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    // Getters
    public AuditInfo getAuditInfo() { return auditInfo; }
    public SecurityValidation getSecurityValidation() { return securityValidation; }
    public Map<String, Object> getAllIAMMetadata() { return Map.copyOf(iamSpecificMetadata); }
    public String getOrganizationId() { return organizationId; }
    public String getTenantId() { return tenantId; }
    public boolean isSensitiveDataIncluded() { return sensitiveDataIncluded; }
    public ComplianceInfo getComplianceInfo() { return complianceInfo; }
    
    // Setters
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    // ✅ 내부 클래스 제거 완료 - 별도 파일로 분리됨
    
    @Override
    public String toString() {
        return String.format("IAMResponse{id='%s', type='%s', status=%s, audit=%s, security=%s}", 
                getResponseId(), getResponseType(), getStatus(), 
                auditInfo.isAuditRequired(), securityValidation.isValidated());
    }
}
