package io.spring.aicore.components.retriever;

import io.spring.aicore.protocol.AIRequest;
import io.spring.iam.aiam.protocol.types.ConditionTemplateContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * 조건 템플릿 생성을 위한 전용 RAG 검색 구현체
 * 
 * ✅ ContextRetriever 확장
 * 🔍 조건 템플릿 관련 컨텍스트 정보 검색
 * 📚 기존 조건 템플릿, 메서드 패턴, SpEL 표현식 등 검색
 */
@Slf4j
@Component
public class ConditionTemplateContextRetriever extends ContextRetriever {
    
    public ConditionTemplateContextRetriever(VectorStore vectorStore) {
        super(vectorStore);
    }
    
    /**
     * 조건 템플릿 컨텍스트 특화 검색
     */
    public String retrieveConditionTemplateContext(AIRequest<ConditionTemplateContext> request) {
        log.info("🔍 조건 템플릿 컨텍스트 검색 시작: {}", request.getRequestId());
        
        try {
            ConditionTemplateContext context = request.getContext();
            
            if ("universal".equals(context.getTemplateType())) {
                return retrieveUniversalTemplateContext(context);
            } else if ("specific".equals(context.getTemplateType())) {
                return retrieveSpecificTemplateContext(context);
            } else {
                log.warn("⚠️ 알 수 없는 템플릿 타입: {}", context.getTemplateType());
                return getDefaultContext();
            }
            
        } catch (Exception e) {
            log.error("🔥 조건 템플릿 컨텍스트 검색 실패", e);
            return getDefaultContext();
        }
    }
    
    /**
     * 범용 조건 템플릿용 컨텍스트 검색
     */
    private String retrieveUniversalTemplateContext(ConditionTemplateContext context) {
        log.debug("🌐 범용 조건 템플릿 컨텍스트 검색");
        
        // 실제 구현에서는 VectorStore나 Database에서 검색
        return """
        ## 범용 조건 템플릿 컨텍스트
        
        ### Spring Security 기본 표현식
        - isAuthenticated(): 사용자 인증 상태 확인
        - hasRole('ROLE_ADMIN'): 특정 역할 보유 확인
        - hasAuthority('READ_PRIVILEGE'): 특정 권한 보유 확인
        
        ### 시간 기반 제약
        - T(java.time.LocalTime).now().hour >= 9: 업무시간 제약
        - T(java.time.DayOfWeek).from(T(java.time.LocalDate).now()) != T(java.time.DayOfWeek).SATURDAY: 평일 제약
        
        ### ABAC 속성 기반
        - #authentication.principal.department == 'IT': 부서 기반 제약
        - #request.remoteAddr.startsWith('192.168.'): IP 기반 제약
        
        ### 네이밍 가이드라인
        - "~상태 확인": 인증/권한 상태
        - "~역할 확인": 역할 기반 제약  
        - "~접근 제한": 시간/위치 기반 제약
        - "권한" 용어 사용 금지!
        """;
    }
    
    /**
     * 특화 조건 템플릿용 컨텍스트 검색
     */
    private String retrieveSpecificTemplateContext(ConditionTemplateContext context) {
        log.debug("🎯 특화 조건 템플릿 컨텍스트 검색: {}", context.getResourceIdentifier());
        
        String resourceIdentifier = context.getResourceIdentifier();
        String methodInfo = context.getMethodInfo();
        
        // 실제 구현에서는 메서드 시그니처 분석, 기존 조건 검색 등
        return String.format("""
        ## 특화 조건 템플릿 컨텍스트
        
        ### 대상 리소스: %s
        ### 메서드 정보: %s
        
        ### hasPermission 사용 패턴
        
        **ID 파라미터인 경우 (3개 파라미터 필수):**
        - hasPermission(#id, 'GROUP', 'READ'): Long id로 그룹 읽기
        - hasPermission(#id, 'USER', 'DELETE'): Long id로 사용자 삭제
        - hasPermission(#idx, 'GROUP', 'UPDATE'): Long idx로 그룹 수정
        
        **객체 파라미터인 경우 (2개 파라미터 필수):**
        - hasPermission(#group, 'CREATE'): Group 객체 생성
        - hasPermission(#userDto, 'UPDATE'): UserDto 객체 수정
        - hasPermission(#document, 'DELETE'): Document 객체 삭제
        
        ### 네이밍 가이드라인
        - "~대상 검증": 객체/ID 기반 검증
        - "~접근 확인": 리소스 접근 확인
        - "권한" 용어 절대 사용 금지!
        
        ### 금지 사항
        - 3개 이상 파라미터 hasPermission
        - 존재하지 않는 파라미터 사용
        - 복합 조건 (&&, ||) 사용
        """, resourceIdentifier != null ? resourceIdentifier : "UNKNOWN", 
            methodInfo != null ? methodInfo : "UNKNOWN");
    }
    
    /**
     * 기본 컨텍스트 반환
     */
    private String getDefaultContext() {
        return """
        ## 기본 조건 템플릿 컨텍스트
        
        ### 기본 지침
        - Spring Security 표준 표현식 사용
        - hasPermission() 함수 중심 활용
        - 간단하고 명확한 조건 생성
        - "권한" 용어 사용 금지
        
        ### 기본 패턴
        - 인증 확인: isAuthenticated()
        - 역할 확인: hasRole('ROLE_ADMIN')
        - 리소스 접근: hasPermission(#param, 'ACTION')
        """;
    }
} 