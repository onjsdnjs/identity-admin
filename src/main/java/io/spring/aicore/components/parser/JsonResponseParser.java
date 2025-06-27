package io.spring.aicore.components.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class JsonResponseParser {

    private final ObjectMapper objectMapper;
    
    public JsonResponseParser() {
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
        
        // 대체 패턴들 시도
        String[] patterns = {
            "\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}",
            "```json\\s*(\\{.*?\\})\\s*```",
            "```\\s*(\\{.*?\\})\\s*```"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
            Matcher m = pattern.matcher(aiResponse);
            if (m.find()) {
                return m.group(1).trim();
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

    /**
     * 타입별 JSON 파싱을 수행합니다
     */
    public <T> T parseToType(String jsonStr, Class<T> targetType) {
        try {
            String cleanJson = extractAndCleanJson(jsonStr);
            return objectMapper.readValue(cleanJson, targetType);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("JSON 파싱 실패", e);
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