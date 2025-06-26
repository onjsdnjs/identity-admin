package io.spring.identityadmin.aiam.operations;

import io.spring.identityadmin.aiam.protocol.IAMContext;
import io.spring.identityadmin.aiam.protocol.IAMRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * IAM 보안 검증기
 * 
 * 🎯 최고 수준의 보안 검증
 * - 요청 권한 검증
 * - 컨텍스트 보안 검사
 * - 위험 패턴 감지
 * - 규정 준수 확인
 */
@Component
public class IAMSecurityValidator {
    
    /**
     * IAM 요청의 보안성을 검증합니다
     */
    public <T extends IAMContext> void validateRequest(IAMRequest<T> request, SecurityContext securityContext) {
        // 1. 기본 보안 검증
        validateBasicSecurity(request, securityContext);
        
        // 2. 컨텍스트별 보안 검증
        validateContextSecurity(request.getContext(), securityContext);
        
        // 3. 위험 패턴 검사
        checkRiskPatterns(request, securityContext);
        
        // 4. 규정 준수 검사
        validateCompliance(request, securityContext);
    }
    
    // ==================== Private Validation Methods ====================
    
    private <T extends IAMContext> void validateBasicSecurity(IAMRequest<T> request, SecurityContext securityContext) {
        // 인증 확인
        if (securityContext.getAuthentication() == null) {
            throw new SecurityException("Authentication required");
        }
        
        if (!securityContext.getAuthentication().isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        // 요청 무결성 확인
        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new SecurityException("Request ID is required for security tracking");
        }
        
        // 타임스탬프 검증 (재생 공격 방지)
        LocalDateTime requestTimestamp = request.getTimestamp();
        long requestTime = requestTimestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - requestTime);
        
        if (timeDiff > 300000) { // 5분 이상 차이
            throw new SecurityException("Request timestamp is too old or too far in future");
        }
    }
    
    private <T extends IAMContext> void validateContextSecurity(T context, SecurityContext securityContext) {
        // 컨텍스트 보안 레벨 확인
        if (context.getSecurityLevel() == null) {
            throw new SecurityException("Security level must be specified");
        }
        
        // 사용자 권한과 요청된 보안 레벨 매칭
        String userRole = getUserHighestRole(securityContext);
        if (!isAuthorizedForSecurityLevel(userRole, context.getSecurityLevel())) {
            throw new SecurityException("Insufficient privileges for requested security level: " + context.getSecurityLevel());
        }
        
        // 감사 요구사항 확인
        if (context.getAuditRequirement() == null) {
            throw new SecurityException("Audit requirement must be specified");
        }
    }
    
    private <T extends IAMContext> void checkRiskPatterns(IAMRequest<T> request, SecurityContext securityContext) {
        String username = securityContext.getAuthentication().getName();
        
        // 비정상적인 요청 패턴 감지
        if (isAbnormalRequestPattern(username, request)) {
            throw new SecurityException("Abnormal request pattern detected for user: " + username);
        }
        
        // 권한 상승 시도 감지
        if (isPrivilegeEscalationAttempt(request, securityContext)) {
            throw new SecurityException("Potential privilege escalation attempt detected");
        }
        
        // 대량 요청 감지
        if (isBulkRequestSuspicious(request, securityContext)) {
            throw new SecurityException("Suspicious bulk request pattern detected");
        }
    }
    
    private <T extends IAMContext> void validateCompliance(IAMRequest<T> request, SecurityContext securityContext) {
        // GDPR 준수 확인
        if (containsPersonalData(request) && !hasGDPRConsent(securityContext)) {
            throw new SecurityException("GDPR consent required for processing personal data");
        }
        
        // SOX 준수 확인 (금융 데이터)
        if (containsFinancialData(request) && !hasSOXAuthorization(securityContext)) {
            throw new SecurityException("SOX authorization required for financial data access");
        }
        
        // HIPAA 준수 확인 (의료 데이터)
        if (containsHealthData(request) && !hasHIPAAAuthorization(securityContext)) {
            throw new SecurityException("HIPAA authorization required for health data access");
        }
    }
    
    // ==================== Helper Methods ====================
    
    private String getUserHighestRole(SecurityContext securityContext) {
        return securityContext.getAuthentication().getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .max(this::compareRoles)
                .orElse("ROLE_USER");
    }
    
    private int compareRoles(String role1, String role2) {
        // 역할 우선순위 정의
        int priority1 = getRolePriority(role1);
        int priority2 = getRolePriority(role2);
        return Integer.compare(priority1, priority2);
    }
    
    private int getRolePriority(String role) {
        switch (role) {
            case "ROLE_SUPER_ADMIN": return 100;
            case "ROLE_ADMIN": return 90;
            case "ROLE_IAM_MANAGER": return 80;
            case "ROLE_SECURITY_OFFICER": return 70;
            case "ROLE_AUDITOR": return 60;
            case "ROLE_MANAGER": return 50;
            case "ROLE_USER": return 10;
            default: return 0;
        }
    }
    
    private boolean isAuthorizedForSecurityLevel(String userRole, Object securityLevel) {
        int userPriority = getRolePriority(userRole);
        int requiredPriority = getSecurityLevelRequiredPriority(securityLevel);
        return userPriority >= requiredPriority;
    }
    
    private int getSecurityLevelRequiredPriority(Object securityLevel) {
        String level = securityLevel.toString();
        switch (level) {
            case "TOP_SECRET": return 100;
            case "SECRET": return 90;
            case "CONFIDENTIAL": return 80;
            case "RESTRICTED": return 70;
            case "INTERNAL": return 50;
            case "PUBLIC": return 10;
            default: return 50;
        }
    }
    
    // 위험 패턴 감지 메서드들
    private <T extends IAMContext> boolean isAbnormalRequestPattern(String username, IAMRequest<T> request) {
        // 실제 구현에서는 Redis나 데이터베이스를 통한 패턴 분석
        return false; // 임시 구현
    }
    
    private <T extends IAMContext> boolean isPrivilegeEscalationAttempt(IAMRequest<T> request, SecurityContext securityContext) {
        // 권한 상승 패턴 분석
        return false; // 임시 구현
    }
    
    private <T extends IAMContext> boolean isBulkRequestSuspicious(IAMRequest<T> request, SecurityContext securityContext) {
        // 대량 요청 패턴 분석
        return false; // 임시 구현
    }
    
    // 규정 준수 체크 메서드들
    private <T extends IAMContext> boolean containsPersonalData(IAMRequest<T> request) {
        // 개인정보 포함 여부 확인
        return false; // 임시 구현
    }
    
    private boolean hasGDPRConsent(SecurityContext securityContext) {
        // GDPR 동의 확인
        return true; // 임시 구현
    }
    
    private <T extends IAMContext> boolean containsFinancialData(IAMRequest<T> request) {
        // 금융 데이터 포함 여부 확인
        return false; // 임시 구현
    }
    
    private boolean hasSOXAuthorization(SecurityContext securityContext) {
        // SOX 권한 확인
        return true; // 임시 구현
    }
    
    private <T extends IAMContext> boolean containsHealthData(IAMRequest<T> request) {
        // 의료 데이터 포함 여부 확인
        return false; // 임시 구현
    }
    
    private boolean hasHIPAAAuthorization(SecurityContext securityContext) {
        // HIPAA 권한 확인
        return true; // 임시 구현
    }
} 