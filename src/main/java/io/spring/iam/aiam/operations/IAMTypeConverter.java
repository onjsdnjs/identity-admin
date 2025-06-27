package io.spring.iam.aiam.operations;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * IAM과 AI Core 간의 타입 변환기
 * 
 * 🎯 완벽한 타입 안전성 보장
 * - IAM 타입 ↔ AI Core 타입 양방향 변환
 * - 메타데이터 보존
 * - 성능 최적화된 변환
 */
@Component
public class IAMTypeConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(IAMTypeConverter.class);
    
    /**
     * IAM 요청을 AI Core 요청으로 변환
     */
    public <T extends IAMContext> AIRequest<T> toAIRequest(IAMRequest<T> iamRequest) {
        // IAMRequest는 AIRequest를 확장하므로 안전한 캐스팅
        return (AIRequest<T>) iamRequest;
    }
    
    /**
     * 범용 요청을 IAM 요청으로 변환 (타입 안전성)
     */
    public <T extends IAMContext> IAMRequest<T> toIAMRequest(Object request) {
        if (request instanceof IAMRequest) {
            return (IAMRequest<T>) request;
        }
        
        // 필요시 다른 타입의 요청을 IAMRequest로 변환하는 로직 추가
        throw new IllegalArgumentException("Cannot convert " + request.getClass() + " to IAMRequest");
    }
    
    /**
     * IAM 응답 타입에 대응하는 AI Core 응답 타입 반환
     */
    public <R extends IAMResponse> Class<? extends AIResponse> toCoreResponseType(Class<R> iamResponseType) {
        // IAMResponse는 AIResponse를 확장하므로 안전한 캐스팅
        return (Class<? extends AIResponse>) iamResponseType;
    }
    
    /**
     * AI Core 응답을 IAM 응답으로 변환
     */
    public <R extends IAMResponse> R toIAMResponse(AIResponse coreResponse, Class<R> targetType) {
        if (targetType.isInstance(coreResponse)) {
            return (R) coreResponse;
        }
        
        // 타입 변환이 필요한 경우의 로직
        throw new IllegalArgumentException("Cannot convert " + coreResponse.getClass() + " to " + targetType);
    }
    
    /**
     * 메타데이터 보존하며 응답 변환
     */
    public <R extends IAMResponse> R convertWithMetadata(AIResponse source, Class<R> targetType) {
        R converted = toIAMResponse(source, targetType);
        
        // 메타데이터 복사 - IAMResponse에 실제로 존재하는 메서드들만 사용
        if (converted instanceof IAMResponse && source instanceof IAMResponse) {
            IAMResponse iamSource = (IAMResponse) source;
            IAMResponse iamTarget = (IAMResponse) converted;
            
            // 기본 메타데이터만 복사 (실제 존재하는 메서드들)
            // TODO: IAMResponse에 실제 메타데이터 메서드들이 추가되면 여기서 복사
            
            // 현재는 기본 필드들만 복사 가능
            // iamTarget.setRequestId(iamSource.getRequestId()); // 이미 생성자에서 설정됨
            // iamTarget.setStatus(iamSource.getStatus()); // 이미 생성자에서 설정됨
        }
        
        return converted;
    }
    
    /**
     * IAM 응답 간 메타데이터를 복사합니다
     * @param source 소스 응답
     * @param target 타겟 응답
     * @param <S> 소스 타입
     * @param <T> 타겟 타입
     */
    public static <S extends IAMResponse, T extends IAMResponse> void copyMetadata(S source, T target) {
        if (source == null || target == null) {
            return;
        }
        
        try {
            // ==================== 완전한 메타데이터 복사 구현 ====================
            
            // 1. 기본 AI 응답 메타데이터 복사
            copyBasicMetadata(source, target);
            
            // 2. IAM 특화 메타데이터 복사
            copyIAMSpecificMetadata(source, target);
            
            // 3. 조직/테넌트 정보 복사
            copyOrganizationMetadata(source, target);
            
            // 4. 보안 및 감사 메타데이터 복사
            copySecurityMetadata(source, target);
            
            // 5. 컴플라이언스 정보 복사
            copyComplianceMetadata(source, target);
            
        } catch (Exception e) {
            // 메타데이터 복사 실패 시 로깅하고 계속 진행
            logger.warn("Failed to copy metadata from {} to {}: {}", 
                source.getClass().getSimpleName(), 
                target.getClass().getSimpleName(), 
                e.getMessage());
        }
    }
    
    /**
     * 기본 AI 응답 메타데이터를 복사합니다
     */
    private static <S extends IAMResponse, T extends IAMResponse> void copyBasicMetadata(S source, T target) {
        // AIResponse 레벨의 메타데이터 복사
        Map<String, Object> sourceMetadata = source.getAllMetadata();
        if (sourceMetadata != null && !sourceMetadata.isEmpty()) {
            sourceMetadata.forEach((key, value) -> {
                try {
                    target.withMetadata(key, value);
                } catch (Exception e) {
                    logger.debug("Failed to copy basic metadata key '{}': {}", key, e.getMessage());
                }
            });
        }
        
        // 실행 시간 정보 복사 (있는 경우)
        if (source.getExecutionTime() != null) {
            target.withExecutionTime(source.getExecutionTime());
        }
        
        // 신뢰도 점수 복사
        if (source.getConfidenceScore() > 0.0) {
            target.withConfidenceScore(source.getConfidenceScore());
        }
        
        // AI 모델 정보 복사
        if (source.getAiModel() != null && !source.getAiModel().trim().isEmpty()) {
            target.withAiModel(source.getAiModel());
        }
        
        // 경고 메시지 복사
        if (source.hasWarnings()) {
            target.withWarnings(source.getWarnings());
        }
        
        // 에러 메시지 복사
        if (source.getErrorMessage() != null && !source.getErrorMessage().trim().isEmpty()) {
            target.withError(source.getErrorMessage());
        }
    }
    
    /**
     * IAM 특화 메타데이터를 복사합니다
     */
    private static <S extends IAMResponse, T extends IAMResponse> void copyIAMSpecificMetadata(S source, T target) {
        // IAM 특화 메타데이터 전체 복사
        Map<String, Object> iamMetadata = source.getAllIAMMetadata();
        if (iamMetadata != null && !iamMetadata.isEmpty()) {
            iamMetadata.forEach((key, value) -> {
                try {
                    target.withIAMMetadata(key, value);
                } catch (Exception e) {
                    logger.debug("Failed to copy IAM metadata key '{}': {}", key, e.getMessage());
                }
            });
        }
        
        // 민감 데이터 플래그 복사
        if (source.isSensitiveDataIncluded()) {
            target.withSensitiveDataFlag(true);
        }
    }
    
    /**
     * 조직 및 테넌트 메타데이터를 복사합니다
     */
    private static <S extends IAMResponse, T extends IAMResponse> void copyOrganizationMetadata(S source, T target) {
        // 조직 ID 복사
        if (source.getOrganizationId() != null && !source.getOrganizationId().trim().isEmpty()) {
            target.withOrganizationId(source.getOrganizationId());
        }
        
        // 테넌트 ID 복사
        if (source.getTenantId() != null && !source.getTenantId().trim().isEmpty()) {
            target.setTenantId(source.getTenantId());
        }
    }
    
    /**
     * 보안 및 감사 메타데이터를 복사합니다
     */
    private static <S extends IAMResponse, T extends IAMResponse> void copySecurityMetadata(S source, T target) {
        // 감사 정보 복사
        AuditInfo sourceAuditInfo = source.getAuditInfo();
        AuditInfo targetAuditInfo = target.getAuditInfo();
        
        if (sourceAuditInfo != null && targetAuditInfo != null) {
            try {
                // 감사 필수 여부 복사
                if (sourceAuditInfo.isAuditRequired()) {
                    targetAuditInfo.setAuditRequired(true);
                }
                
                // 감사 사용자와 액션 복사 (가능한 경우)
                if (sourceAuditInfo.getUserId() != null && sourceAuditInfo.getAction() != null) {
                    targetAuditInfo.recordAction(sourceAuditInfo.getUserId(), sourceAuditInfo.getAction());
                }
                
            } catch (Exception e) {
                logger.debug("Failed to copy audit info: {}", e.getMessage());
            }
        }
        
        // 보안 검증 정보 복사
        SecurityValidation sourceSecurityValidation = source.getSecurityValidation();
        SecurityValidation targetSecurityValidation = target.getSecurityValidation();
        
        if (sourceSecurityValidation != null && targetSecurityValidation != null) {
            try {
                // 검증 상태 복사
                if (sourceSecurityValidation.isValidated()) {
                    targetSecurityValidation.markValidated(
                        sourceSecurityValidation.getValidator() != null ? sourceSecurityValidation.getValidator() : "SYSTEM",
                        sourceSecurityValidation.getValidationLevel() != null ? sourceSecurityValidation.getValidationLevel() : "BASIC"
                    );
                }
                
            } catch (Exception e) {
                logger.debug("Failed to copy security validation info: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 컴플라이언스 메타데이터를 복사합니다
     */
    private static <S extends IAMResponse, T extends IAMResponse> void copyComplianceMetadata(S source, T target) {
        ComplianceInfo sourceCompliance = source.getComplianceInfo();
        
        if (sourceCompliance != null) {
            try {
                // 전체 컴플라이언스 정보 복사
                ComplianceInfo targetCompliance = new ComplianceInfo();
                
                // 컴플라이언스 프레임워크 복사
                if (sourceCompliance.getComplianceFramework() != null && !sourceCompliance.getComplianceFramework().trim().isEmpty()) {
                    targetCompliance.setComplianceFramework(sourceCompliance.getComplianceFramework());
                }
                
                // 컴플라이언스 체크 결과 복사
                Map<String, Boolean> complianceChecks = sourceCompliance.getComplianceChecks();
                if (complianceChecks != null && !complianceChecks.isEmpty()) {
                    complianceChecks.forEach((checkName, passed) -> {
                        targetCompliance.addComplianceCheck(checkName, passed);
                    });
                }
                
                target.withComplianceInfo(targetCompliance);
                
            } catch (Exception e) {
                logger.debug("Failed to copy compliance info: {}", e.getMessage());
            }
        }
    }
} 