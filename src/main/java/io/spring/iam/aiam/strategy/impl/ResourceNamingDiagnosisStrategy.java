package io.spring.iam.aiam.strategy.impl;

import io.spring.iam.aiam.labs.LabAccessor;
import io.spring.iam.aiam.labs.resource.ResourceNamingLab;
import io.spring.iam.aiam.protocol.IAMContext;
import io.spring.iam.aiam.protocol.IAMRequest;
import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.iam.aiam.protocol.enums.DiagnosisType;
import io.spring.iam.aiam.protocol.request.ResourceNamingSuggestionRequest;
import io.spring.iam.aiam.protocol.response.ResourceNamingSuggestionResponse;
import io.spring.iam.aiam.strategy.DiagnosisException;
import io.spring.iam.aiam.strategy.DiagnosisStrategy;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🔬 리소스 네이밍 진단 전략
 *
 * ✅ 동적 Lab 접근 패턴 사용!
 * LabAccessor를 통한 타입 안전한 동적 Lab 조회
 *
 * 🎯 역할:
 * 1. 요청 데이터 검증 및 전처리
 * 2. LabAccessor를 통한 동적 Lab 조회
 * 3. ResourceNamingLab에 작업 위임 (Pipeline 활용)
 * 4. 결과 후처리 및 응답 생성
 */
@Slf4j
@Component
public class ResourceNamingDiagnosisStrategy implements DiagnosisStrategy<IAMContext, IAMResponse> {

    private final LabAccessor labAccessor;

    public ResourceNamingDiagnosisStrategy(LabAccessor labAccessor) {
        this.labAccessor = labAccessor;
        log.info("🔬 ResourceNamingDiagnosisStrategy initialized with dynamic LabAccessor");
    }

    @Override
    public DiagnosisType getSupportedType() {
        return DiagnosisType.RESOURCE_NAMING;
    }

    @Override
    public int getPriority() {
        return 10; // 높은 우선순위
    }

    @Override
    public IAMResponse execute(IAMRequest<IAMContext> request, Class<IAMResponse> responseType) throws DiagnosisException {
        log.info("🔬 리소스 네이밍 진단 전략 실행 시작 - 요청: {}", request.getRequestId());

        try {
            // 1. 요청 데이터 검증
            validateRequest(request);

            // 2. 동적 Lab 조회
            Optional<ResourceNamingLab> labOpt = labAccessor.getLab(ResourceNamingLab.class);
            if (labOpt.isEmpty()) {
                throw new DiagnosisException("RESOURCE_NAMING", "LAB_NOT_FOUND",
                        "ResourceNamingLab을 찾을 수 없습니다");
            }

            ResourceNamingLab resourceNamingLab = labOpt.get();

            // 3. 리소스 네이밍 요청 구성
            ResourceNamingSuggestionRequest namingRequest = buildNamingRequest(request);

            // 4. Lab에 작업 위임 (6단계 파이프라인 실행)
            ResourceNamingSuggestionResponse namingResponse = resourceNamingLab.processResourceNaming(namingRequest);

            // 5. 응답 생성
            ResourceNamingResponse response = new ResourceNamingResponse(
                    request.getRequestId(),
                    ExecutionStatus.SUCCESS,
                    namingResponse
            );

            log.info("✅ 리소스 네이밍 진단 전략 실행 완료 - 요청: {}", request.getRequestId());
            return response;

        } catch (DiagnosisException e) {
            throw e; // 이미 DiagnosisException인 경우 그대로 전파
        } catch (Exception e) {
            log.error("🔥 리소스 네이밍 진단 전략 실행 실패 - 요청: {}", request.getRequestId(), e);
            throw new DiagnosisException("RESOURCE_NAMING", "EXECUTION_FAILED",
                    "리소스 네이밍 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 요청 데이터 검증
     */
    private void validateRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> resources = (List<Map<String, String>>) request.getParameter("resources", List.class);
        
        if (resources == null || resources.isEmpty()) {
            throw new DiagnosisException("RESOURCE_NAMING", "MISSING_RESOURCES",
                    "resources 파라미터가 필요합니다");
        }
    }

    /**
     * 리소스 네이밍 요청 구성
     */
    private ResourceNamingSuggestionRequest buildNamingRequest(IAMRequest<IAMContext> request) throws DiagnosisException {
        try {
            @SuppressWarnings("unchecked") 
            List<Map<String, String>> legacyResources = (List<Map<String, String>>) request.getParameter("resources", List.class);
            
            // Map 리스트에서 신버전 형식으로 변환
            return ResourceNamingSuggestionRequest.fromMapList(legacyResources);
            
        } catch (ClassCastException e) {
            throw new DiagnosisException("RESOURCE_NAMING", "INVALID_RESOURCES_FORMAT",
                    "resources 파라미터 형식이 올바르지 않습니다", e);
        }
    }

    /**
     * 리소스 네이밍 응답 클래스
     */
    public static class ResourceNamingResponse extends IAMResponse {
        private final ResourceNamingSuggestionResponse namingResult;

        public ResourceNamingResponse(String requestId, ExecutionStatus status,
                                     ResourceNamingSuggestionResponse namingResult) {
            super(requestId, status);
            this.namingResult = namingResult;
        }

        @Override
        public String getResponseType() {
            return "RESOURCE_NAMING";
        }

        @Override
        public Object getData() {
            return Map.of(
                    "suggestions", namingResult.getSuggestions(),
                    "stats", namingResult.getStats(),
                    "failedIdentifiers", namingResult.getFailedIdentifiers(),
                    "timestamp", getTimestamp(),
                    "requestId", getRequestId()
            );
        }

        public ResourceNamingSuggestionResponse getNamingResult() { return namingResult; }

        @Override
        public String toString() {
            return String.format("ResourceNamingResponse{requestId='%s', status='%s', suggestions=%d}",
                    getResponseId(), getStatus(), namingResult.getSuggestions().size());
        }
    }
} 