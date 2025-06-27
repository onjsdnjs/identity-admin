package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.stereotype.Component;

/**
 * ✅ OCP 준수: 정책 생성 특화 프롬프트 템플릿
 * 
 * 새로운 정책 생성 템플릿 추가 시:
 * 1. 이 클래스를 복사
 * 2. @PromptTemplateConfig의 key와 aliases 수정
 * 3. 프롬프트 내용 수정
 * 4. PromptGenerator 코드는 수정 불필요!
 */
@Component
@PromptTemplateConfig(
    key = "generatePolicyFromText",
    aliases = {"generatePolicyFromTextStream", "policy_generation"},
    description = "IAM 정책 생성을 위한 프롬프트 템플릿"
)
public class PolicyGenerationTemplate implements PromptTemplate {
    
    @Override
    public String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
        return String.format("""
            당신은 IAM 정책 분석 AI '아비터'입니다. 
            
            🎯 임무: 자연어 요구사항을 분석하여 구체적인 정책 구성 요소로 변환
            
            📋 시스템 정보:
            %s
            
            ⚠️ 절대적 JSON 규칙 (위반 시 시스템 오류 발생):
            1. JSON에는 절대 주석을 포함하지 마세요 (// 또는 /* */ 절대 금지)
            2. JSON 내부에 설명 텍스트 절대 금지
            3. 각 필드는 한 번만 포함 (중복 절대 금지)
            4. 모든 ID는 반드시 숫자만 사용
            5. 문자열 값은 반드시 쌍따옴표로 감싸기
            6. 마지막 항목 뒤에 쉼표 절대 금지
            7. 빈 값은 빈 문자열("")이나 빈 배열([]) 사용
            
            📤 필수 JSON 형식 (정확히 이 형식만 사용):
            
            ===JSON시작===
            {
              "policyName": "정책이름",
              "description": "정책설명", 
              "roleIds": [2],
              "permissionIds": [3],
              "conditions": {"1": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]},
              "aiRiskAssessmentEnabled": false,
              "requiredTrustScore": 0.7,
              "customConditionSpel": "",
              "effect": "ALLOW"
            }
            ===JSON끝===
            
            분석 과정이나 설명은 JSON 블록 앞에 작성하고, JSON은 완벽하게 파싱 가능한 형태로만 작성하세요.
            """, systemMetadata);
    }
    
    @Override
    public String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
        String naturalLanguageQuery = extractQueryFromRequest(request);
        
        return String.format("""
            **자연어 요구사항:**
            "%s"
            
            **참고 컨텍스트:**
            %s
            
            위 요구사항을 분석하여 정책을 구성해주세요.
            """, naturalLanguageQuery, contextInfo);
    }
    
    private String extractQueryFromRequest(AIRequest<? extends DomainContext> request) {
        return request.toString();
    }
} 