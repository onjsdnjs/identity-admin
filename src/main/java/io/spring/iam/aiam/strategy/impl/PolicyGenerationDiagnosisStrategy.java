package io.spring.iam.aiam.strategy.impl;

import io.spring.iam.aiam.labs.LabAccessor;
import io.spring.iam.aiam.labs.policy.AdvancedPolicyGenerationLab;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.protocol.types.PolicyContext;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.aiam.strategy.DiagnosisStrategy;
import io.spring.iam.domain.dto.PolicyDto;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 🏭 정책 생성 진단 전략
 *
 * ✅ 동적 Lab 접근 패턴 사용!
 * LabAccessor를 통한 타입 안전한 동적 Lab 조회
 *
 * 🎯 역할:
 * 1. 요청 데이터 검증 및 전처리
 * 2. LabAccessor를 통한 동적 Lab 조회
 * 3. AdvancedPolicyGenerationLab에 작업 위임 (Pipeline 활용)
 * 4. 결과 후처리 및 응답 생성
 */
@Slf4j
@Component
public class PolicyGenerationDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {

    private final LabAccessor labAccessor;

    public PolicyGenerationDiagnosisStrategy(LabAccessor labAccessor) {
        this.labAccessor = labAccessor;
        log.info("🏭 PolicyGenerationDiagnosisStrategy initialized with dynamic LabAccessor");
    }

    @Override
    public DiagnosisType getSupportedType() {
        return DiagnosisType.POLICY_GENERATION;
    }

    @Override
    public int getPriority() {
        return 20; // 높은 우선순위
    }

    @Override
    public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) throws DiagnosisException {
        log.info("🏭 정책 생성 진단 전략 실행 시작 - 요청: {}", request.getRequestId());

        try {
            // 1. 요청 데이터 검증
            validateRequest(request);

            // 2. 동적 Lab 조회
            Optional<AdvancedPolicyGenerationLab> labOpt = labAccessor.getLab(AdvancedPolicyGenerationLab.class);
            if (labOpt.isEmpty()) {
                throw new DiagnosisException("POLICY_GENERATION", "LAB_NOT_FOUND",
                        "AdvancedPolicyGenerationLab을 찾을 수 없습니다");
            }

            AdvancedPolicyGenerationLab policyGenerationLab = labOpt.get();

            // 3. 자연어 쿼리 추출
            String naturalLanguageQuery = request.getParameter("naturalLanguageQuery", String.class);
            String generationMode = request.getParameter("generationMode", String.class);

            // 4. Pipeline 기반 Lab에 작업 위임
            log.debug("🏭 AdvancedPolicyGenerationLab에 정책 생성 작업 위임 (Pipeline 활용)");
            PolicyDto policyDto;

            if ("context_aware".equals(generationMode)) {
                // 컨텍스트 기반 정책 생성
                PolicyContext policyContext = convertToPolicyContext(request);
                policyDto = policyGenerationLab.generateContextAwarePolicy(policyContext, naturalLanguageQuery);
            } else {
                // 일반 고급 정책 생성
                policyDto = policyGenerationLab.generateAdvancedPolicy(naturalLanguageQuery);
            }

            // 5. 응답 생성
            PolicyGenerationResponse response = new PolicyGenerationResponse(
                    request.getRequestId(),
                    ExecutionStatus.SUCCESS,
                    policyDto,
                    generationMode,
                    naturalLanguageQuery
            );

            log.info("✅ 정책 생성 진단 전략 실행 완료 - 요청: {}", request.getRequestId());
            return response;

        } catch (DiagnosisException e) {
            throw e; // 이미 DiagnosisException인 경우 그대로 전파
        } catch (Exception e) {
            log.error("🔥 정책 생성 진단 전략 실행 실패 - 요청: {}", request.getRequestId(), e);
            throw new DiagnosisException("POLICY_GENERATION", "EXECUTION_FAILED",
                    "정책 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        if (request.getParameter("generationMode", String.class) == null) {
            throw new DiagnosisException("POLICY_GENERATION", "MISSING_GENERATION_MODE",
                    "generationMode 파라미터가 필요합니다");
        }

        if (request.getParameter("naturalLanguageQuery", String.class) == null) {
            throw new DiagnosisException("POLICY_GENERATION", "MISSING_NATURAL_LANGUAGE_QUERY",
                    "naturalLanguageQuery 파라미터가 필요합니다");
        }

        String query = request.getParameter("naturalLanguageQuery", String.class);
        if (query.trim().isEmpty()) {
            throw new DiagnosisException("POLICY_GENERATION", "EMPTY_NATURAL_LANGUAGE_QUERY",
                    "자연어 쿼리가 비어있습니다");
        }
    }

    /**
     * IAMContext를 PolicyContext로 변환
     */
    private PolicyContext convertToPolicyContext(IAMRequest<IAMContext> request) {
        IAMContext context = request.getContext();

        PolicyContext policyContext = new PolicyContext(
                context.getSecurityLevel(),
                context.getAuditRequirement()
        );

        // 자연어 쿼리 설정
        String naturalLanguageQuery = request.getParameter("naturalLanguageQuery", String.class);
        policyContext.setNaturalLanguageQuery(naturalLanguageQuery);

        // 보안 컨텍스트 복사
        policyContext.setSecurityContext(context.getSecurityContext());

        return policyContext;
    }

    /**
     * 정책 생성 응답 클래스
     */
    public static class PolicyGenerationResponse extends IAMResponse {
        private final PolicyDto policyDto;
        private final String generationMode;
        private final String originalQuery;

        public PolicyGenerationResponse(String requestId, ExecutionStatus status,
                                        PolicyDto policyDto, String generationMode, String originalQuery) {
            super(requestId, status);
            this.policyDto = policyDto;
            this.generationMode = generationMode;
            this.originalQuery = originalQuery;
        }

        @Override
        public String getResponseType() {
            return "POLICY_GENERATION";
        }

        @Override
        public Object getData() {
            return Map.of(
                    "policy", policyDto != null ? policyDto : new Object(),
                    "policyName", policyDto != null ? policyDto.getName() : "",
                    "policyDescription", policyDto != null ? policyDto.getDescription() : "",
                    "policyEffect", policyDto != null ? policyDto.getEffect().name() : "",
                    "generationMode", generationMode != null ? generationMode : "",
                    "originalQuery", originalQuery != null ? originalQuery : "",
                    "timestamp", getTimestamp(),
                    "requestId", getRequestId()
            );
        }

        public PolicyDto getPolicyDto() { return policyDto; }

        public String getGenerationMode() { return generationMode; }

        public String getOriginalQuery() { return originalQuery; }

        @Override
        public String toString() {
            return String.format("PolicyGenerationResponse{requestId='%s', status='%s', mode='%s', policy='%s'}",
                    getResponseId(), getStatus(), generationMode,
                    policyDto != null ? policyDto.getName() : "null");
        }
    }
} 