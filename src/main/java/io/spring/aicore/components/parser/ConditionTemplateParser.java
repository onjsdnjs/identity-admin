package io.spring.aicore.components.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.protocol.response.ConditionTemplateGenerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 응답에서 JSON을 추출하고 정제하는 실제 구현체
 * 
 * 🔧 현재 하드코딩된 JSON 파싱 로직을 체계화
 * - AI 응답에서 JSON 블록 추출
 * - JSON 구조 수정 및 정제
 * - 다양한 파싱 전략 지원
 */
@Slf4j
@Component
public class ConditionTemplateParser implements ResponseParser {

    private final ObjectMapper objectMapper;
    
    public ConditionTemplateParser() {
        this.objectMapper = createLenientObjectMapper();
    }

    /**
     * AI 응답에서 JSON을 추출하고 정제합니다 (현재 하드코딩된 로직과 동일)
     */
    public String extractAndCleanJson(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "{}";
        }

        try {
            log.debug("🔍 AI 응답에서 JSON 추출 시작");
            
            // 1. JSON 블록 추출 (===JSON시작=== ~ ===JSON끝=== 패턴)
            String jsonBlock = extractJsonBlock(aiResponse);
            if (jsonBlock == null) {
                log.warn("JSON 블록을 찾을 수 없음, 전체 응답에서 JSON 추출 시도");
                jsonBlock = aiResponse;
            }
            
            // 2. JSON 정제
            String cleanedJson = cleanJsonString(jsonBlock);
            
            // 3. JSON 구조 수정
            String fixedJson = fixJsonStructure(cleanedJson);
            
            // 4. 최종 검증
            validateJson(fixedJson);
            
            log.debug("✅ JSON 추출 및 정제 완료");
            return fixedJson;
            
        } catch (Exception e) {
            log.error("❌ JSON 추출 실패: {}", e.getMessage());
            return createFallbackJson();
        }
    }

    /**
     * JSON 블록 추출 (현재 하드코딩된 로직과 동일)
     */
    private String extractJsonBlock(String aiResponse) {
        Pattern jsonBlockPattern = Pattern.compile("===JSON시작===(.*?)===JSON끝===", Pattern.DOTALL);
        Matcher matcher = jsonBlockPattern.matcher(aiResponse);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 대체 패턴들 시도 (🔥 수정: 모든 패턴에 그룹 추가)
        String[] patterns = {
            "(\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\})",  // 🔥 그룹 추가
            "```json\\s*(\\{.*?\\})\\s*```",
            "```\\s*(\\{.*?\\})\\s*```"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
            Matcher m = pattern.matcher(aiResponse);
            if (m.find()) {
                return m.group(1).trim();  // 이제 group(1)이 존재함
            }
        }
        
        return null;
    }

    /**
     * JSON 문자열을 정제합니다 (현재 하드코딩된 로직과 동일)
     */
    public String cleanJsonString(String jsonStr) {
        if (jsonStr == null) return "{}";
        
        // 1. 주석 제거
        String cleaned = removeJsonComments(jsonStr);
        
        // 2. 불필요한 공백 정리
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        // 3. 특수 문자 이스케이프 처리
        cleaned = cleaned.replace("\\\"", "\"");
        
        return cleaned;
    }

    /**
     * JSON에서 주석을 제거합니다 (현재 하드코딩된 로직과 동일)
     */
    public String removeJsonComments(String json) {
        if (json == null) return "{}";
        
        // 1. 한 줄 주석 제거 (//)
        json = json.replaceAll("//.*?(?=\\r?\\n|$)", "");
        
        // 2. 블록 주석 제거 (/* */)
        json = json.replaceAll("/\\*.*?\\*/", "");
        
        return json;
    }

    /**
     * JSON 구조를 수정합니다 (현재 하드코딩된 로직과 동일)
     */
    public String fixJsonStructure(String json) {
        if (json == null) return "{}";
        
        try {
            // 1. 기본 구조 검증
            if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
                json = "{" + json + "}";
            }
            
            // 2. 마지막 쉼표 제거
            json = json.replaceAll(",\\s*}", "}");
            json = json.replaceAll(",\\s*]", "]");
            
            // 3. 키 따옴표 추가
            json = json.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");
            
            return json;
            
        } catch (Exception e) {
            log.warn("JSON 구조 수정 실패: {}", e.getMessage());
            return json;
        }
    }

    @Override
    public <T> T parse(String jsonResponse, T target) {
        return null;
    }

    /**
     * 타입별 JSON 파싱을 수행합니다
     * 
     * ✅ ResourceNaming 실책 방지: 조건 템플릿 특화 타입 처리
     */
    public <T> T parseToType(String jsonStr, Class<T> targetType) {
        try {
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                log.warn("🔥 parseToType 입력이 비어있음, null 반환");
                return null;
            }
            
            log.debug("🔍 parseToType 시작: targetType={}, json length={}", targetType.getSimpleName(), jsonStr.length());
            
            // ConditionTemplateGenerationResponse 직접 처리
            if (targetType.isAssignableFrom(ConditionTemplateGenerationResponse.class)) {
                log.debug("🎯 ConditionTemplateGenerationResponse 타입 감지, 조건 템플릿 파싱 시작");
                return parseConditionTemplateResponse(jsonStr, targetType);
            }
            
            // AIResponse는 추상 클래스이므로 구체 타입으로 변환
            if (targetType == AIResponse.class || AIResponse.class.isAssignableFrom(targetType)) {
                log.debug("🎯 AIResponse 추상 타입 감지, ConditionTemplateGenerationResponse로 변환");
                return parseConditionTemplateResponse(jsonStr, targetType);
            }
            
            // 기본 JSON 파싱
            String cleanJson = extractAndCleanJson(jsonStr);
            return objectMapper.readValue(cleanJson, targetType);
            
        } catch (Exception e) {
            log.error("🔥 parseToType 실패: targetType={}, json length={}", targetType.getSimpleName(), jsonStr.length(), e);
            return null;
        }
    }
    
    /**
     * 조건 템플릿 응답을 파싱합니다
     */
    @SuppressWarnings("unchecked")
    private <T> T parseConditionTemplateResponse(String jsonStr, Class<T> targetType) {
        try {
            log.debug("🔍 조건 템플릿 응답 파싱 시작");
            
            // JSON에서 템플릿 배열 추출
            String templateJson = extractTemplateArrayFromResponse(jsonStr);
            
            if (templateJson == null || templateJson.trim().isEmpty()) {
                log.warn("🔥 템플릿 JSON이 비어있음, 실패 응답 생성");
                ConditionTemplateGenerationResponse failureResponse = ConditionTemplateGenerationResponse.failure(
                        "unknown", "unknown", null, "Empty template JSON");
                return targetType.cast(failureResponse);
            }
            
            // 성공 응답 생성
            ConditionTemplateGenerationResponse response = ConditionTemplateGenerationResponse.success(
                    "pipeline-generated", templateJson, "auto-detected", null);
            
            log.debug("✅ 조건 템플릿 응답 파싱 완료: hasTemplates={}", response.hasTemplates());
            return targetType.cast(response);
            
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 응답 파싱 실패", e);
            
            // 실패 응답 생성
            ConditionTemplateGenerationResponse failureResponse = ConditionTemplateGenerationResponse.failure(
                    "unknown", "unknown", null, "Parsing failed: " + e.getMessage());
            return targetType.cast(failureResponse);
        }
    }
    
    /**
     * AI 응답에서 템플릿 배열을 추출합니다
     */
    private String extractTemplateArrayFromResponse(String jsonStr) {
        try {
            log.debug("🔍 템플릿 배열 추출 시작");
            
            // 1. 마크다운 코드 블록 제거
            String cleaned = jsonStr.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            
            // 2. JSON 배열 패턴 찾기
            Pattern arrayPattern = Pattern.compile("\\[\\s*\\{.*?\\}\\s*\\]", Pattern.DOTALL);
            Matcher arrayMatcher = arrayPattern.matcher(cleaned);
            
            if (arrayMatcher.find()) {
                String found = arrayMatcher.group().trim();
                log.debug("✅ JSON 배열 패턴으로 추출 성공: {} characters", found.length());
                return found;
            }
            
            // 3. 전체가 배열인지 확인
            cleaned = cleaned.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                log.debug("✅ 전체 JSON이 배열 형태: {} characters", cleaned.length());
                return cleaned;
            }
            
            // 4. 단일 객체를 배열로 감싸기
            Pattern objectPattern = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
            Matcher objectMatcher = objectPattern.matcher(cleaned);
            
            if (objectMatcher.find()) {
                String objectJson = objectMatcher.group().trim();
                String arrayJson = "[" + objectJson + "]";
                log.debug("✅ 단일 객체를 배열로 변환: {} characters", arrayJson.length());
                return arrayJson;
            }
            
            log.warn("🔥 유효한 JSON 구조를 찾을 수 없음");
            return "[]";
            
        } catch (Exception e) {
            log.error("🔥 템플릿 배열 추출 실패", e);
            return "[]";
        }
    }

    /**
     * Map 형태로 JSON을 파싱합니다
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseToMap(String jsonStr) {
        return parseToType(jsonStr, Map.class);
    }
    
    /**
     * JSON 유효성 검증
     */
    private void validateJson(String json) throws JsonProcessingException {
        objectMapper.readTree(json);
    }
    
    /**
     * 관대한 ObjectMapper 생성 (현재 하드코딩된 로직과 동일)
     */
    private ObjectMapper createLenientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        return mapper;
    }
    
    /**
     * 폴백 JSON 생성
     */
    private String createFallbackJson() {
        return """
            {
              "policyName": "AI 생성 실패",
              "description": "JSON 파싱 오류로 인한 기본 정책",
              "roleIds": [],
              "permissionIds": [],
              "conditions": {},
              "aiRiskAssessmentEnabled": false,
              "requiredTrustScore": 0.5,
              "customConditionSpel": "",
              "effect": "DENY"
            }
            """;
    }
}