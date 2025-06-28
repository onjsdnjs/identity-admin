package io.spring.iam.aiam.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.iam.aiam.labs.LabAccessor;
import io.spring.iam.aiam.labs.condition.ConditionTemplateGenerationLab;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.protocol.response.ConditionTemplateGenerationResponse;
import io.spring.iam.aiam.protocol.response.StringResponse;
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
 * **ResourceNaming 실책 방지 적용:**
 * ✅ 타입 안전성: 구체적인 Response 타입 사용
 * ✅ null 안전성: 모든 단계에서 null 체크
 * ✅ Lab 응답 처리: 새로운 ConditionTemplateGenerationResponse 처리
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
    private final ObjectMapper objectMapper;

    public ConditionTemplateDiagnosisStrategy(LabAccessor labAccessor) {
        this.labAccessor = labAccessor;
        this.objectMapper = new ObjectMapper();
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
            ConditionTemplateGenerationResponse labResponse;

            if ("universal".equals(templateType)) {
                log.debug("🔬 범용 조건 템플릿 생성 요청");
                labResponse = conditionTemplateGenerationLab.generateUniversalConditionTemplates();
            } else if ("specific".equals(templateType)) {
                log.debug("🔬 특화 조건 템플릿 생성 요청");
                String resourceIdentifier = request.getParameter("resourceIdentifier", String.class);
                String methodInfo = request.getParameter("methodInfo", String.class);
                labResponse = conditionTemplateGenerationLab.generateSpecificConditionTemplates(resourceIdentifier, methodInfo);
            } else {
                throw new DiagnosisException("CONDITION_TEMPLATE", "INVALID_TEMPLATE_TYPE",
                        "지원하지 않는 템플릿 타입입니다: " + templateType);
            }

            // 4. ✅ null 안전성 보장
            if (labResponse == null) {
                log.warn("🔥 Lab에서 null 응답 수신");
                throw new DiagnosisException("CONDITION_TEMPLATE", "NULL_RESPONSE",
                        "Lab에서 null 응답을 반환했습니다");
            }

            // 5. ✅ StringResponse로 응답 생성 (JSON 직렬화)
            Map<String, Object> responseData = Map.of(
                    "templates", labResponse.getTemplateResult() != null ? labResponse.getTemplateResult() : "",
                    "templateType", templateType != null ? templateType : "",
                    "resourceIdentifier", labResponse.getResourceIdentifier() != null ? labResponse.getResourceIdentifier() : "",
                    "metadata", labResponse.getProcessingMetadata(),
                    "timestamp", System.currentTimeMillis(),
                    "requestId", request.getRequestId(),
                    "responseType", "CONDITION_TEMPLATE"
            );
            
            String jsonContent = objectMapper.writeValueAsString(responseData);
            StringResponse response = new StringResponse(request.getRequestId(), jsonContent);

            log.info("✅ 조건 템플릿 진단 전략 실행 완료 - 응답: CONDITION_TEMPLATE");
            return response;

        } catch (DiagnosisException e) {
            log.error("🔥 조건 템플릿 진단 전략 실행 실패 (DiagnosisException): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 진단 전략 실행 실패 (Exception)", e);
            throw new DiagnosisException("CONDITION_TEMPLATE", "EXECUTION_ERROR",
                    "조건 템플릿 진단 실행 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        if (request == null) {
            throw new DiagnosisException("CONDITION_TEMPLATE", "NULL_REQUEST", "요청이 null입니다");
        }

        String templateType = request.getParameter("templateType", String.class);
        if (templateType == null || templateType.trim().isEmpty()) {
            throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_TEMPLATE_TYPE", 
                    "templateType 파라미터가 필요합니다");
        }

        if ("specific".equals(templateType)) {
            String resourceIdentifier = request.getParameter("resourceIdentifier", String.class);
            if (resourceIdentifier == null || resourceIdentifier.trim().isEmpty()) {
                throw new DiagnosisException("CONDITION_TEMPLATE", "MISSING_RESOURCE_IDENTIFIER", 
                        "특화 조건 템플릿에는 resourceIdentifier가 필요합니다");
            }
        }
    }

} 