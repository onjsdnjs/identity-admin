package io.spring.iam.aiam.strategy.impl;

import io.spring.iam.aiam.labs.LabAccessor;
import io.spring.iam.aiam.labs.condition.ConditionTemplateGenerationLab;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.aiam.strategy.DiagnosisStrategy;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 🔬 조건 템플릿 진단 전략
 *
 * ✅ 동적 Lab 접근 패턴 사용!
 * LabAccessor를 통한 타입 안전한 동적 Lab 조회
 *
 * 🎯 역할:
 * 1. 요청 데이터 검증 및 전처리
 * 2. LabAccessor를 통한 동적 Lab 조회
 * 3. ConditionTemplateGenerationLab에 작업 위임 (Pipeline 활용)
 * 4. 결과 후처리 및 응답 생성
 */
@Slf4j
@Component
public class ConditionTemplateDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {

    private final LabAccessor labAccessor;

    public ConditionTemplateDiagnosisStrategy(LabAccessor labAccessor) {
        this.labAccessor = labAccessor;
        log.info("🔬 ConditionTemplateDiagnosisStrategy initialized with dynamic LabAccessor");
    }

    @Override
    public DiagnosisType getSupportedType() {
        return DiagnosisType.CONDITION_TEMPLATE;
    }

    @Override
    public int getPriority() {
        return 15; // 중간 우선순위
    }

    @Override
    public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) throws DiagnosisException {
        log.info("🔬 조건 템플릿 진단 전략 실행 시작 - 요청: {}", request.getRequestId());

        try {
            // 1. 요청 데이터 검증
            validateRequest(request);

            // 2. 동적 Lab 조회
            Optional<ConditionTemplateGenerationLab> labOpt = labAccessor.getLab(ConditionTemplateGenerationLab.class);
            if (labOpt.isEmpty()) {
                throw new DiagnosisException("CONDITION_TEMPLATE", "LAB_NOT_FOUND",
                        "ConditionTemplateGenerationLab을 찾을 수 없습니다");
            }

            ConditionTemplateGenerationLab conditionTemplateGenerationLab = labOpt.get();

            // 3. 템플릿 타입 확인 및 생성
            String templateType = request.getParameter("templateType", String.class);
            String result;

            if ("universal".equals(templateType)) {
                log.debug("🔬 범용 조건 템플릿 생성 요청");
                result = conditionTemplateGenerationLab.generateUniversalConditionTemplates();
            } else if ("specific".equals(templateType)) {
                log.debug("🔬 특화 조건 템플릿 생성 요청");
                String resourceIdentifier = request.getParameter("resourceIdentifier", String.class);
                String methodInfo = request.getParameter("methodInfo", String.class);
                result = conditionTemplateGenerationLab.generateSpecificConditionTemplates(resourceIdentifier, methodInfo);
            } else {
                throw new DiagnosisException("CONDITION_TEMPLATE", "INVALID_TEMPLATE_TYPE",
                        "지원하지 않는 템플릿 타입입니다: " + templateType);
            }

            // 4. 응답 생성
            ConditionTemplateResponse response = new ConditionTemplateResponse(
                    request.getRequestId(),
                    ExecutionStatus.SUCCESS,
                    result,
                    templateType
            );

            log.info("✅ 조건 템플릿 진단 전략 실행 완료 - 요청: {}", request.getRequestId());
            return response;

        } catch (DiagnosisException e) {
            throw e; // 이미 DiagnosisException인 경우 그대로 전파
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 진단 전략 실행 실패 - 요청: {}", request.getRequestId(), e);
            throw new DiagnosisException("CONDITION_TEMPLATE", "EXECUTION_FAILED",
                    "조건 템플릿 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        String templateType = request.getParameter("templateType", String.class);
        if (templateType == null) {
            throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_TEMPLATE_TYPE",
                    "templateType 파라미터가 필요합니다");
        }

        if ("specific".equals(templateType)) {
            String resourceIdentifier = request.getParameter("resourceIdentifier", String.class);
            if (resourceIdentifier == null || resourceIdentifier.trim().isEmpty()) {
                throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_RESOURCE_IDENTIFIER",
                        "specific 템플릿 타입에는 resourceIdentifier 파라미터가 필요합니다");
            }
        }
    }

    /**
     * 조건 템플릿 응답 클래스
     */
    public static class ConditionTemplateResponse extends IAMResponse {
        private final String templateResult;
        private final String templateType;

        public ConditionTemplateResponse(String requestId, ExecutionStatus status,
                                         String templateResult, String templateType) {
            super(requestId, status);
            this.templateResult = templateResult;
            this.templateType = templateType;
        }

        @Override
        public String getResponseType() {
            return "CONDITION_TEMPLATE";
        }

        @Override
        public Object getData() {
            return Map.of(
                    "templates", templateResult != null ? templateResult : "",
                    "templateType", templateType != null ? templateType : "",
                    "timestamp", getTimestamp(),
                    "requestId", getRequestId()
            );
        }

        public String getTemplateResult() { return templateResult; }

        public String getTemplateType() { return templateType; }

        @Override
        public String toString() {
            return String.format("ConditionTemplateResponse{requestId='%s', status='%s', type='%s'}",
                    getResponseId(), getStatus(), templateType);
        }
    }
} 