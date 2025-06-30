package io.spring.aicore.components.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.aicore.protocol.AIResponse;
import io.spring.iam.aiam.dto.ResourceNameSuggestion;
import io.spring.iam.aiam.protocol.request.ResourceNamingSuggestionRequest;
import io.spring.iam.aiam.protocol.response.ResourceNamingSuggestionResponse;
import io.spring.iam.aiam.protocol.response.StringResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 리소스 네이밍 AI 응답 JSON 파서
 * 구버전의 복잡한 다중 파싱 전략을 캡슐화
 */
@Slf4j
@Component
public class ResourceNamingJsonParser implements ResponseParser {

    private final ObjectMapper objectMapper;

    public ResourceNamingJsonParser() {
        this.objectMapper = createLenientObjectMapper();
    }

    public ResourceNamingSuggestionResponse parse(String jsonResponse, Object originalRequest) {
        long startTime = System.currentTimeMillis();
        log.info("ResourceNaming JSON 파싱 시작 - 응답 길이: {}", jsonResponse != null ? jsonResponse.length() : 0);

        if (!(originalRequest instanceof ResourceNamingSuggestionRequest)) {
            log.error("잘못된 요청 타입: {}", originalRequest.getClass());
            return createEmptyResponse();
        }

        ResourceNamingSuggestionRequest request = (ResourceNamingSuggestionRequest) originalRequest;

        try {
            // 🔥 구버전 완전 이식: parseAiResponseEnhanced 호출
            Map<String, ResourceNameSuggestion> rawMap = parseAiResponseEnhanced(jsonResponse, request.getResources());
            
            // 🔥 ResourceNameSuggestion을 Map<String, Map<String, String>>으로 변환
            Map<String, Map<String, String>> convertedMap = new HashMap<>();
            for (Map.Entry<String, ResourceNameSuggestion> entry : rawMap.entrySet()) {
                Map<String, String> values = new HashMap<>();
                values.put("friendlyName", entry.getValue().friendlyName());
                values.put("description", entry.getValue().description());
                convertedMap.put(entry.getKey(), values);
            }

            // 4단계: 응답 DTO로 변환
            ResourceNamingSuggestionResponse response = convertToResponse(convertedMap);
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.getStats().setProcessingTimeMs(processingTime);
            
            log.info("JSON 파싱 완료 - 성공: {}, 실패: {}, 처리시간: {}ms", 
                    response.getStats().getSuccessfullyProcessed(),
                    response.getStats().getFailed(),
                    processingTime);

            return response;

        } catch (Exception e) {
            log.error("JSON 파싱 완전 실패", e);
            return createEmptyResponse();
        }
    }

    /**
     * 🔥 구버전 완전 이식: parseAiResponseEnhanced 메서드 (4단계 파싱 전략)
     */
    private Map<String, ResourceNameSuggestion> parseAiResponseEnhanced(String jsonResponse, List<ResourceNamingSuggestionRequest.ResourceItem> originalBatch) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        try {
            // 1단계: JSON 정제
            String cleanedJson = cleanJsonResponse(jsonResponse);
            log.debug("🔥 정제된 JSON: {}", cleanedJson);

            // 2단계: 다양한 파싱 전략 시도
            result = tryMultipleParsingStrategiesEnhanced(cleanedJson);

            // 3단계: 파싱 결과 검증
            if (result.isEmpty()) {
                log.warn("🔥 모든 파싱 전략 실패, 정규식 파싱 시도");
                result = regexParsingEnhanced(cleanedJson);
            }

            // 4단계: 누락된 항목 확인 및 보완 - AI 디버깅을 위해 주석처리
            Set<String> requestedIdentifiers = originalBatch.stream()
                    .map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier)
                    .collect(Collectors.toSet());

            Set<String> parsedIdentifiers = result.keySet();
            Set<String> missingIdentifiers = new HashSet<>(requestedIdentifiers);
            missingIdentifiers.removeAll(parsedIdentifiers);

            if (!missingIdentifiers.isEmpty()) {
                log.error("🔥 [AI 오류] 파싱 후에도 누락된 항목: {}", missingIdentifiers);
            }

        } catch (Exception e) {
            log.error("🔥 강화된 파싱 실패", e);

            // AI 오류를 명확히 파악하기 위해 빈 결과 반환
            log.error("🔥 [AI 오류] 모든 파싱 전략 실패");
        }

        return result;
    }

    /**
     * 🔥 구버전 완전 이식: 다양한 파싱 전략 시도 (Enhanced 버전)
     */
    private Map<String, ResourceNameSuggestion> tryMultipleParsingStrategiesEnhanced(String json) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        // 전략 1: 표준 ObjectMapper
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            Map<String, Map<String, String>> parsed = mapper.readValue(
                    json,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );

            for (Map.Entry<String, Map<String, String>> entry : parsed.entrySet()) {
                String friendlyName = entry.getValue().get("friendlyName");
                String description = entry.getValue().get("description");

                if (friendlyName != null && description != null) {
                    result.put(entry.getKey(), new ResourceNameSuggestion(friendlyName, description));
                }
            }

            if (!result.isEmpty()) {
                log.info("🔥 표준 파싱 성공, 항목 수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("🔥 표준 파싱 실패: {}", e.getMessage());
        }

        // 전략 2: JsonNode 사용
        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

            if (root.isObject()) {
                Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                    String key = field.getKey();
                    com.fasterxml.jackson.databind.JsonNode value = field.getValue();

                    if (value.has("friendlyName") && value.has("description")) {
                        String friendlyName = value.get("friendlyName").asText();
                        String description = value.get("description").asText();
                        result.put(key, new ResourceNameSuggestion(friendlyName, description));
                    }
                }
            }

            if (!result.isEmpty()) {
                log.info("🔥 JsonNode 파싱 성공, 항목 수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("🔥 JsonNode 파싱 실패: {}", e.getMessage());
        }

        // 🔥 구버전 parseAiResponse 전체 6단계 전략 호출
        try {
            Map<String, Map<String, String>> rawMap = parseAiResponse(json);
            result = convertToResourceNameSuggestions(rawMap);
            if (!result.isEmpty()) {
                log.info("🔥 parseAiResponse 6단계 전략 성공, 항목 수: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.debug("🔥 parseAiResponse 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 🔥 구버전 완전 이식: AI 응답을 파싱하는 개선된 메서드 (6단계 파싱 전략)
     */
    private Map<String, Map<String, String>> parseAiResponse(String jsonStr) throws Exception {
        log.debug("🔥 parseAiResponse 파싱 시작, JSON 길이: {}, 첫 100자: {}",
                jsonStr.length(),
                jsonStr.substring(0, Math.min(100, jsonStr.length())));

        // 빈 JSON 체크
        if (jsonStr.trim().equals("{}") || jsonStr.trim().isEmpty()) {
            log.warn("🔥 빈 JSON 응답 감지");
            return new HashMap<>();
        }

        // 더 유연한 ObjectMapper 사용 (구버전과 완전 동일)
        ObjectMapper lenientMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        try {
            // 1차 시도: 일반 파싱
            Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                    jsonStr,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );

            // Map으로 반환 (신버전에 맞게 변환)
            return rawResponseMap;

        } catch (Exception e) {
            log.warn("🔥 1차 파싱 실패, 복구 시도: {}", e.getMessage());

            // 2차 시도: JSON 구조 분석 후 복구
            String analyzedJson = analyzeAndFixJsonStructure(jsonStr);

            if (analyzedJson != null && !analyzedJson.equals(jsonStr)) {
                try {
                    Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                            analyzedJson,
                            new TypeReference<Map<String, Map<String, String>>>() {}
                    );
                    return rawResponseMap;
                } catch (Exception e2) {
                    log.warn("🔥 구조 분석 후 파싱도 실패: {}", e2.getMessage());
                }
            }

            // 3차 시도: JSON 복구
            String repairedJson = repairJson(jsonStr);
            log.debug("🔥 복구된 JSON: {}", repairedJson);

            try {
                Map<String, Map<String, String>> rawResponseMap = lenientMapper.readValue(
                        repairedJson,
                        new TypeReference<Map<String, Map<String, String>>>() {}
                );

                return rawResponseMap;
            } catch (Exception e3) {
                log.error("🔥 3차 파싱도 실패: {}", e3.getMessage());

                // 4차 시도: 수동 파싱
                return manualJsonParse(jsonStr);
            }
        }
    }

    /**
     * 🔥 구버전 완전 이식: JSON 구조 분석 및 수정
     */
    private String analyzeAndFixJsonStructure(String json) {
        try {
            // 잘못된 형식 패턴 감지 및 수정
            // 패턴 1: {"friendlyName": "이름", "description": "설명"} 형태가 최상위에 있는 경우
            if (json.trim().startsWith("{") && json.contains("\"friendlyName\"") && !json.contains(":{")) {
                log.info("🔥 잘못된 JSON 구조 감지: 최상위에 friendlyName이 직접 있음");
                // 임시 키로 감싸기
                return "{\"temp_key\": " + json + "}";
            }

            // 패턴 2: 값이 문자열로만 되어 있는 경우
            // 예: {"key": "value"} -> {"key": {"friendlyName": "value", "description": "설명 없음"}}
            ObjectMapper mapper = new ObjectMapper();
            try {
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                if (root.isObject()) {
                    com.fasterxml.jackson.databind.node.ObjectNode newRoot = mapper.createObjectNode();
                    Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = root.fields();

                    while (fields.hasNext()) {
                        Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                        String key = field.getKey();
                        com.fasterxml.jackson.databind.JsonNode value = field.getValue();

                        if (value.isTextual()) {
                            // 문자열 값을 객체로 변환
                            com.fasterxml.jackson.databind.node.ObjectNode newValue = mapper.createObjectNode();
                            newValue.put("friendlyName", value.asText());
                            newValue.put("description", "AI가 설명을 제공하지 않았습니다.");
                            newRoot.set(key, newValue);
                        } else if (value.isObject() && (!value.has("friendlyName") || !value.has("description"))) {
                            // 필수 필드가 없는 객체 수정
                            com.fasterxml.jackson.databind.node.ObjectNode objValue = (com.fasterxml.jackson.databind.node.ObjectNode) value;
                            if (!objValue.has("friendlyName")) {
                                objValue.put("friendlyName", key);
                            }
                            if (!objValue.has("description")) {
                                objValue.put("description", "설명 없음");
                            }
                            newRoot.set(key, objValue);
                        } else {
                            newRoot.set(key, value);
                        }
                    }

                    return mapper.writeValueAsString(newRoot);
                }
            } catch (Exception e) {
                log.debug("🔥 JSON 구조 분석 중 오류: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("🔥 JSON 구조 수정 실패: {}", e.getMessage());
        }

        return json;
    }

    /**
     * 🔥 구버전 완전 이식: JSON 복구 메서드 (개선된 버전)
     */
    private String repairJson(String json) {
        String repaired = json.trim();

        // 1. 잘못된 백슬래시 수정
        repaired = repaired.replaceAll("\\\\(?![\"\\\\nrtbf/])", "\\\\\\\\");

        // 2. 잘못된 쉼표 제거
        repaired = repaired.replaceAll(",\\s*}", "}");
        repaired = repaired.replaceAll(",\\s*]", "]");

        // 3. 이스케이프되지 않은 따옴표 처리
        // 문자열 내부의 따옴표만 이스케이프
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < repaired.length(); i++) {
            char c = repaired.charAt(i);

            if (!escaped && c == '"') {
                if (inString && i + 1 < repaired.length() && repaired.charAt(i + 1) == '"') {
                    // 연속된 따옴표 발견
                    sb.append("\\\"");
                    i++; // 다음 따옴표 건너뛰기
                } else {
                    inString = !inString;
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }

            escaped = (c == '\\' && !escaped);
        }

        repaired = sb.toString();

        // 4. 줄바꿈 문자 이스케이프
        if (!repaired.contains("\\n")) {
            repaired = repaired.replaceAll("\n", "\\\\n");
        }
        if (!repaired.contains("\\r")) {
            repaired = repaired.replaceAll("\r", "\\\\r");
        }

        // 5. 불완전한 JSON 마무리
        long openBraces = repaired.chars().filter(c -> c == '{').count();
        long closeBraces = repaired.chars().filter(c -> c == '}').count();

        while (openBraces > closeBraces) {
            // 마지막 항목이 완전한지 확인
            int lastComma = repaired.lastIndexOf(',');
            int lastCloseBrace = repaired.lastIndexOf('}');

            if (lastComma > lastCloseBrace) {
                // 불완전한 항목 제거
                repaired = repaired.substring(0, lastComma);
            }

            repaired += "}";
            closeBraces++;
        }

        return repaired;
    }

    /**
     * 🔥 구버전 완전 이식: 수동 JSON 파싱 (최후의 수단) - 4가지 패턴
     */
    private Map<String, Map<String, String>> manualJsonParse(String json) {
        log.info("🔥 수동 JSON 파싱 시작");
        Map<String, Map<String, String>> result = new HashMap<>();

        try {
            // 여러 패턴 시도
            List<Pattern> patterns = Arrays.asList(
                    // 패턴 1: 표준 형식
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 2: description이 먼저 오는 경우
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 3: 한 필드만 있는 경우 (friendlyName만)
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                            Pattern.MULTILINE | Pattern.DOTALL
                    ),
                    // 패턴 4: 단순 키-값 형태
                    Pattern.compile(
                            "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"",
                            Pattern.MULTILINE
                    )
            );

            for (int i = 0; i < patterns.size(); i++) {
                Pattern pattern = patterns.get(i);
                Matcher matcher = pattern.matcher(json);

                while (matcher.find()) {
                    String identifier = matcher.group(1);

                    if (i == 0) {
                        // 표준 형식
                        String friendlyName = matcher.group(2);
                        String description = matcher.group(3);
                        Map<String, String> values = new HashMap<>();
                        values.put("friendlyName", friendlyName.trim());
                        values.put("description", description.trim());
                        result.put(identifier.trim(), values);
                    } else if (i == 1) {
                        // description이 먼저
                        String description = matcher.group(2);
                        String friendlyName = matcher.group(3);
                        Map<String, String> values = new HashMap<>();
                        values.put("friendlyName", friendlyName.trim());
                        values.put("description", description.trim());
                        result.put(identifier.trim(), values);
                    } else if (i == 2) {
                        // friendlyName만
                        String friendlyName = matcher.group(2);
                        Map<String, String> values = new HashMap<>();
                        values.put("friendlyName", friendlyName.trim());
                        values.put("description", "설명 없음");
                        result.put(identifier.trim(), values);
                    } else if (i == 3 && !result.containsKey(identifier)) {
                        // 단순 키-값 (이미 파싱된 항목은 덮어쓰지 않음)
                        String value = matcher.group(2);
                        Map<String, String> values = new HashMap<>();
                        values.put("friendlyName", value.trim());
                        values.put("description", "AI가 설명을 제공하지 않았습니다.");
                        result.put(identifier.trim(), values);
                    }

                    log.debug("🔥 수동 파싱 성공 (패턴 {}): {} -> {}",
                            i + 1, identifier, result.get(identifier.trim()).get("friendlyName"));
                }
            }

            if (result.isEmpty()) {
                log.warn("🔥 수동 파싱으로도 항목을 찾을 수 없음");

                // 최후의 시도: JsonNode로 부분 파싱
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

                    if (root.isObject()) {
                        Iterator<String> fieldNames = root.fieldNames();
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            com.fasterxml.jackson.databind.JsonNode value = root.get(fieldName);

                            if (value.isTextual()) {
                                // 텍스트 값만 있는 경우
                                Map<String, String> values = new HashMap<>();
                                values.put("friendlyName", value.asText());
                                values.put("description", "AI가 설명을 제공하지 않았습니다.");
                                result.put(fieldName, values);
                            } else if (value.isObject()) {
                                // 객체인 경우 가능한 필드 추출
                                String friendlyName = fieldName;
                                String description = "설명 없음";

                                if (value.has("friendlyName")) {
                                    friendlyName = value.get("friendlyName").asText();
                                }
                                if (value.has("description")) {
                                    description = value.get("description").asText();
                                }

                                Map<String, String> values = new HashMap<>();
                                values.put("friendlyName", friendlyName);
                                values.put("description", description);
                                result.put(fieldName, values);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("🔥 JsonNode 파싱도 실패: {}", e.getMessage());
                }
            }

            log.info("🔥 수동 파싱 완료, 찾은 항목 수: {}", result.size());

        } catch (Exception e) {
            log.error("🔥 수동 파싱 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 🔥 구버전 완전 이식: Map을 ResourceNameSuggestion으로 변환
     */
    private Map<String, ResourceNameSuggestion> convertToResourceNameSuggestions(
            Map<String, Map<String, String>> rawResponseMap) {

        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : rawResponseMap.entrySet()) {
            String key = entry.getKey();
            Map<String, String> suggestionData = entry.getValue();

            String friendlyName = suggestionData.get("friendlyName");
            String description = suggestionData.get("description");

            // 필수 필드 검증 (구버전과 동일)
            if (friendlyName == null || friendlyName.trim().isEmpty()) {
                friendlyName = generateFallbackFriendlyName(key);
                log.warn("🔥 friendlyName이 없어 기본값 사용: {}", friendlyName);
            }

            if (description == null || description.trim().isEmpty()) {
                description = "AI가 설명을 생성하지 못했습니다.";
                log.warn("🔥 description이 없어 기본값 사용");
            }

            result.put(key, new ResourceNameSuggestion(friendlyName.trim(), description.trim()));
        }

        return result;
    }

    /**
     * 🔥 구버전 완전 이식: generateFallbackFriendlyName 메서드
     */
    private String generateFallbackFriendlyName(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "알 수 없는 리소스";
        }

        // URL 경로에서 마지막 부분 추출
        if (identifier.startsWith("/")) {
            String[] parts = identifier.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty() && !parts[i].matches("\\{.*\\}")) {
                    return parts[i] + " 기능";
                }
            }
        }

        // 메서드명에서 이름 추출
        if (identifier.contains(".")) {
            String[] parts = identifier.split("\\.");
            String lastPart = parts[parts.length - 1];
            if (lastPart.contains("()")) {
                lastPart = lastPart.replace("()", "");
            }
            // camelCase를 공백으로 분리
            String formatted = lastPart.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
            return formatted + " 기능";
        }

        return identifier + " 기능";
    }

    /**
     * JSON 응답 정제 (구버전 cleanJsonResponse와 동일)
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 1. 마크다운 제거
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        // 2. JSON 앞뒤 텍스트 제거
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        // 3. 이스케이프 문자 정규화
        cleaned = normalizeEscapes(cleaned);

        // 4. 유니코드 이스케이프 처리
        cleaned = decodeUnicode(cleaned);

        // 5. 잘못된 쉼표 제거
        cleaned = cleaned.replaceAll(",\\s*}", "}");
        cleaned = cleaned.replaceAll(",\\s*]", "]");

        return cleaned;
    }

    /**
     * 🔥 구버전 완전 이식: 이스케이프 문자 정규화
     */
    private String normalizeEscapes(String text) {
        // 줄바꿈 정규화
        text = text.replace("\\n", " ");
        text = text.replace("\\r", "");
        text = text.replace("\\t", " ");

        // 연속된 공백 제거
        text = text.replaceAll("\\s+", " ");

        return text;
    }

    /**
     * 🔥 구버전 완전 이식: 유니코드 이스케이프 디코딩
     */
    private String decodeUnicode(String text) {
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf((char) codePoint));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 응답 DTO로 변환
     */
    private ResourceNamingSuggestionResponse convertToResponse(Map<String, Map<String, String>> rawMap) {
        List<ResourceNamingSuggestionResponse.ResourceNamingSuggestion> suggestions = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, String>> entry : rawMap.entrySet()) {
            String identifier = entry.getKey();
            Map<String, String> values = entry.getValue();
            
            String friendlyName = values.get("friendlyName");
            String description = values.get("description");
            
            if (friendlyName != null && description != null) {
                ResourceNamingSuggestionResponse.ResourceNamingSuggestion suggestion = 
                    ResourceNamingSuggestionResponse.ResourceNamingSuggestion.builder()
                        .identifier(identifier)
                        .friendlyName(friendlyName)
                        .description(description)
                        .confidence(0.8) // 기본 신뢰도
                        .build();
                        
                suggestions.add(suggestion);
            }
        }
        
        ResourceNamingSuggestionResponse.ProcessingStats stats = 
            ResourceNamingSuggestionResponse.ProcessingStats.builder()
                .totalRequested(rawMap.size())
                .successfullyProcessed(suggestions.size())
                .failed(rawMap.size() - suggestions.size())
                .build();
        
        return ResourceNamingSuggestionResponse.builder()
                .suggestions(suggestions)
                .failedIdentifiers(new ArrayList<>())
                .stats(stats)
                .build();
    }

    private ObjectMapper createLenientObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    private ResourceNamingSuggestionResponse createEmptyResponse() {
        return ResourceNamingSuggestionResponse.builder()
                .suggestions(List.of())
                .failedIdentifiers(List.of())
                .stats(ResourceNamingSuggestionResponse.ProcessingStats.builder()
                        .totalRequested(0)
                        .successfullyProcessed(0)
                        .failed(0)
                        .build())
                .build();
    }

    public String getParserName() {
        return "resource-naming-json";
    }

    @Override
    public <T> T parseToType(String jsonStr, Class<T> targetType) {
        try {
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return null;
            }
            
            log.debug("🔍 parseToType 시작: targetType={}, json length={}", targetType.getSimpleName(), jsonStr.length());
            
            // ✅ StringResponse 직접 처리 (최우선)
            if (targetType.isAssignableFrom(StringResponse.class)) {
                log.debug("🎯 StringResponse 타입 감지, JSON 직렬화 후 StringResponse 생성");
                
                // 기존 parse 로직으로 ResourceNamingSuggestionResponse 생성
                ResourceNamingSuggestionRequest dummyRequest = ResourceNamingSuggestionRequest.builder()
                        .resources(List.of())
                        .build();
                
                ResourceNamingSuggestionResponse response = parse(jsonStr, dummyRequest);
                
                // ResourceNamingSuggestionResponse를 JSON으로 직렬화하여 StringResponse 생성
                Map<String, Object> responseData = Map.of(
                        "suggestions", response.getSuggestions(),
                        "stats", response.getStats(),
                        "failedIdentifiers", response.getFailedIdentifiers(),
                        "timestamp", System.currentTimeMillis(),
                        "requestId", "resource-naming-parsed",
                        "responseType", "RESOURCE_NAMING"
                );
                
                String jsonContent = objectMapper.writeValueAsString(responseData);
                StringResponse stringResponse = new StringResponse("resource-naming-parsed", jsonContent);
                
                log.debug("✅ StringResponse 생성 완료: contentLength={}", jsonContent.length());
                return targetType.cast(stringResponse);
            }
            
            // AIResponse는 추상 클래스이므로 구체 타입으로 변환
            if (targetType == AIResponse.class || AIResponse.class.isAssignableFrom(targetType)) {
                log.debug("🔥 AIResponse 추상 타입 감지, ResourceNamingSuggestionResponse로 변환");
                
                // 빈 요청 객체 생성하여 기존 parse 메서드 재사용
                ResourceNamingSuggestionRequest dummyRequest = ResourceNamingSuggestionRequest.builder()
                        .resources(List.of())
                        .build();
                
                ResourceNamingSuggestionResponse response = parse(jsonStr, dummyRequest);
                
                // ResourceNamingSuggestionResponse를 AIResponse로 캐스팅할 수 있도록 수정
                if (targetType.isAssignableFrom(ResourceNamingSuggestionResponse.class)) {
                    return targetType.cast(response);
                } else {
                    // AIResponse 타입이 요구되는 경우 StringAIResponse로 래핑
                    StringAIResponse wrappedResponse = new StringAIResponse("unknown", jsonStr);
                    return targetType.cast(wrappedResponse);
                }
            }
            
            // ResourceNamingSuggestionResponse 직접 처리
            if (targetType.isAssignableFrom(ResourceNamingSuggestionResponse.class)) {
                // 빈 요청 객체 생성하여 기존 parse 메서드 재사용
                ResourceNamingSuggestionRequest dummyRequest = ResourceNamingSuggestionRequest.builder()
                        .resources(List.of())
                        .build();
                
                ResourceNamingSuggestionResponse response = parse(jsonStr, dummyRequest);
                return targetType.cast(response);
            }
            
            // 다른 타입은 기본 ObjectMapper로 처리
            return objectMapper.readValue(jsonStr, targetType);
            
        } catch (Exception e) {
            log.error("🔥 parseToType 실패: targetType={}, json length={}", targetType.getSimpleName(), jsonStr.length(), e);
            return null;
        }
    }

    /**
     * String 데이터를 가진 간단한 AIResponse 구현체
     */
    private static class StringAIResponse extends AIResponse {
        private final String data;
        
        public StringAIResponse(String requestId, String data) {
            super(requestId, AIResponse.ExecutionStatus.SUCCESS);
            this.data = data;
        }
        
        @Override
        public Object getData() {
            return data;
        }
        
        @Override
        public String getResponseType() {
            return "STRING_RESPONSE";
        }
    }

    @Override
    public String extractAndCleanJson(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "{}";
        }
        
        try {
            return cleanJsonResponse(aiResponse);
        } catch (Exception e) {
            log.error("🔥 extractAndCleanJson 실패", e);
            return "{}";
        }
    }

    /**
     * 🔥 구버전 완전 이식: 정규식을 사용한 최후의 파싱 (Enhanced 버전)
     */
    private Map<String, ResourceNameSuggestion> regexParsingEnhanced(String json) {
        Map<String, ResourceNameSuggestion> result = new HashMap<>();

        // 패턴: "identifier": {"friendlyName": "name", "description": "desc"}
        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*\\{\\s*\"friendlyName\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]+)\"\\s*\\}",
                Pattern.MULTILINE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String identifier = matcher.group(1);
            String friendlyName = matcher.group(2);
            String description = matcher.group(3);

            if (identifier != null && friendlyName != null && description != null) {
                result.put(identifier, new ResourceNameSuggestion(friendlyName, description));
                log.debug("🔥 정규식 파싱 성공: {} -> {}", identifier, friendlyName);
            }
        }

        log.info("🔥 정규식 파싱 결과, 항목 수: {}", result.size());
        return result;
    }
}