package io.spring.aicore.components.prompt;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import io.spring.iam.aiam.protocol.request.ResourceNamingSuggestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 리소스 네이밍 AI 진단용 프롬프트 템플릿
 * 구버전의 복잡한 한글 프롬프트 엔지니어링을 캡슐화
 */
@Slf4j
@Component
@PromptTemplateConfig(
    key = "resource_naming_suggestion",
    aliases = {"resource_naming", "리소스네이밍"},
    description = "리소스 친화적 이름 생성용 프롬프트"
)
public class ResourceNamingTemplate implements PromptTemplate {

    @Override
    public String generateSystemPrompt(AIRequest<? extends DomainContext> request, String systemMetadata) {
        return buildSystemPrompt();
    }

    @Override
    public String generateUserPrompt(AIRequest<? extends DomainContext> request, String contextInfo) {
        // AIRequest에서 리소스 정보 추출
        @SuppressWarnings("unchecked")
        List<String> identifiers = request.getParameter("identifiers", List.class);
        
        if (identifiers == null || identifiers.isEmpty()) {
            log.warn("리소스 목록이 비어있습니다");
            return "오류: 처리할 리소스가 없습니다";
        }

        return buildUserPromptFromIdentifiers(identifiers, contextInfo);
    }

    /**
     * 🔥 구버전 호환: 직접 ResourceNamingSuggestionRequest 처리
     */
    public PromptGenerationResult generatePrompt(ResourceNamingSuggestionRequest request, String context) {
        if (request.getResources() == null || request.getResources().isEmpty()) {
            log.warn("리소스 목록이 비어있습니다");
            return PromptGenerationResult.builder()
                    .systemPrompt("오류: 처리할 리소스가 없습니다")
                    .userPrompt("오류")
                    .build();
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request.getResources(), context);
        
        log.debug("ResourceNaming 프롬프트 생성 완료 - 리소스 수: {}", request.getResources().size());
        
        return PromptGenerationResult.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .build();
    }

    /**
     * 구버전과 동일한 시스템 프롬프트 (한글 네이밍 전문가)
     */
    private String buildSystemPrompt() {
        return """
            당신은 소프트웨어의 기술적 용어를 일반 비즈니스 사용자가 이해하기 쉬운 이름과 설명으로 만드는 네이밍 전문가입니다.
            
            **매우 중요한 규칙:**
            1. 제공된 모든 항목(identifier)에 대해 예외 없이 응답해야 합니다
            2. 각 항목마다 반드시 friendlyName과 description을 모두 제공해야 합니다
            3. 순수한 JSON 형식으로만 응답하세요 (설명 텍스트 없음)
            4. 한글로 친화적이고 명확한 이름과 설명을 작성하세요
            5. 영문 메서드명도 반드시 포함하여 응답하세요
            6. 입력된 순서대로 모든 항목을 응답하세요
            
            **처리 규칙:**
            - camelCase나 snake_case는 읽기 쉬운 한글로 변환
            - URL 경로는 기능 이름으로 변환 (예: /admin/users → 사용자 관리)
            - 메서드명은 동작을 나타내는 한글로 변환 (예: updateUser → 사용자 정보 수정)
            - CRUD 작업은 명확한 동사 사용 (생성, 조회, 수정, 삭제)
            
            **응답 형식 (반드시 이 형식을 따르세요):**
            {
              "첫번째_identifier": {
                "friendlyName": "친화적 이름",
                "description": "상세 설명"
              },
              "두번째_identifier": {
                "friendlyName": "친화적 이름",
                "description": "상세 설명"
              }
            }
            
            절대 항목을 누락하지 마세요. 모든 입력에 대해 응답하세요.
            """;
    }

    /**
     * 🔥 구버전 완전 이식: 사용자 프롬프트 생성 (소유자 정보 제외)
     */
    private String buildUserPrompt(List<ResourceNamingSuggestionRequest.ResourceItem> resources, String context) {
        StringBuilder userPrompt = new StringBuilder();
        
        // RAG 컨텍스트가 있으면 추가
        if (context != null && !context.trim().isEmpty()) {
            userPrompt.append("**참고 컨텍스트:**\n")
                     .append(context)
                     .append("\n\n");
        }
        
        // 🔥 구버전과 완전 동일: identifier만 번호 매기기 (소유자 정보 제외)
        userPrompt.append("다음 ").append(resources.size()).append("개의 기술 항목에 대해 모두 응답하세요:\n\n");
        
        IntStream.range(0, resources.size())
                .forEach(i -> {
                    ResourceNamingSuggestionRequest.ResourceItem resource = resources.get(i);
                    userPrompt.append(i + 1)
                             .append(". ")
                             .append(resource.getIdentifier())
                             .append("\n");
                });
        
        return userPrompt.toString();
    }

    /**
     * 🔥 AIRequest identifiers에서 프롬프트 생성
     */
    private String buildUserPromptFromIdentifiers(List<String> identifiers, String context) {
        StringBuilder userPrompt = new StringBuilder();
        
        // RAG 컨텍스트가 있으면 추가
        if (context != null && !context.trim().isEmpty()) {
            userPrompt.append("**참고 컨텍스트:**\n")
                     .append(context)
                     .append("\n\n");
        }
        
        // identifier만 번호 매기기
        userPrompt.append("다음 ").append(identifiers.size()).append("개의 기술 항목에 대해 모두 응답하세요:\n\n");
        
        IntStream.range(0, identifiers.size())
                .forEach(i -> {
                    userPrompt.append(i + 1)
                             .append(". ")
                             .append(identifiers.get(i))
                             .append("\n");
                });
        
        return userPrompt.toString();
    }

    public String getTemplateName() {
        return "resource-naming";
    }

    public String getTemplateVersion() {
        return "1.0";
    }
} 