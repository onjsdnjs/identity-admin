package io.spring.aicore.components.parser;

import java.util.Map;

/**
 * AI 응답에서 JSON을 추출하고 정제하는 범용 파서
 * SRP: JSON 파싱과 정제만 담당
 * OCP: 새로운 JSON 처리 방식 추가 가능
 */
public interface JsonResponseParser {

    /**
     * AI 응답에서 JSON을 추출하고 정제합니다
     * @param aiResponse AI의 원시 응답
     * @return 정제된 JSON 문자열
     */
    String extractAndCleanJson(String aiResponse);

    /**
     * JSON 문자열을 정제합니다
     * @param jsonStr 원시 JSON 문자열
     * @return 정제된 JSON 문자열
     */
    String cleanJsonString(String jsonStr);

    /**
     * JSON에서 주석을 제거합니다
     * @param json 주석이 포함된 JSON
     * @return 주석이 제거된 JSON
     */
    String removeJsonComments(String json);

    /**
     * JSON 구조를 수정합니다
     * @param json 구조가 잘못된 JSON
     * @return 구조가 수정된 JSON
     */
    String fixJsonStructure(String json);

    /**
     * 타입별 JSON 파싱을 수행합니다
     * @param jsonStr JSON 문자열
     * @param targetType 목표 타입
     * @return 파싱된 객체
     */
    <T> T parseToType(String jsonStr, Class<T> targetType);

    /**
     * Map 형태로 JSON을 파싱합니다 (ResourceNameSuggestion 등용)
     * @param jsonStr JSON 문자열
     * @return 파싱된 Map
     */
    Map<String, Object> parseToMap(String jsonStr);
}