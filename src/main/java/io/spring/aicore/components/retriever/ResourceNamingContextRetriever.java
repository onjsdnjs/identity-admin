package io.spring.aicore.components.retriever;

import io.spring.iam.aiam.protocol.request.ResourceNamingSuggestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 리소스 네이밍을 위한 컨텍스트 검색기
 * RAG 패턴으로 관련 리소스 네이밍 히스토리를 검색
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceNamingContextRetriever {

    private final VectorStore vectorStore;

    public String retrieveContext(ResourceNamingSuggestionRequest request) {
        if (request.getResources() == null || request.getResources().isEmpty()) {
            log.debug("검색할 리소스가 없습니다");
            return "";
        }

        try {
            // 모든 리소스 식별자를 조합하여 검색 쿼리 생성
            String searchQuery = buildSearchQuery(request);
            log.debug("RAG 검색 쿼리: {}", searchQuery);

            // Vector Store에서 유사한 리소스 네이밍 사례 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(searchQuery)
                    .topK(10) // 상위 10개 유사 사례
                    .similarityThreshold(0.6) // 60% 이상 유사도
                    .build();

            List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
            
            if (similarDocs.isEmpty()) {
                log.debug("유사한 리소스 네이밍 사례를 찾지 못했습니다");
                return "";
            }

            // 검색된 문서들을 컨텍스트로 변환
            String context = buildContextFromDocuments(similarDocs);
            log.debug("RAG 컨텍스트 검색 완료 - 문서 수: {}, 컨텍스트 길이: {}", 
                     similarDocs.size(), context.length());
            
            return context;

        } catch (Exception e) {
            log.error("RAG 컨텍스트 검색 중 오류 발생", e);
            return "";
        }
    }

    /**
     * 리소스 목록에서 검색 쿼리 생성
     */
    private String buildSearchQuery(ResourceNamingSuggestionRequest request) {
        // 리소스 식별자들을 분석하여 검색에 유용한 키워드 추출
        List<String> keywords = request.getResources().stream()
                .map(ResourceNamingSuggestionRequest.ResourceItem::getIdentifier)
                .flatMap(identifier -> extractKeywords(identifier).stream())
                .distinct()
                .collect(Collectors.toList());

        // 서비스 소유자 정보도 추가
        List<String> owners = request.getResources().stream()
                .map(ResourceNamingSuggestionRequest.ResourceItem::getOwner)
                .filter(owner -> owner != null && !owner.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        StringBuilder query = new StringBuilder();
        query.append("리소스 네이밍 사례: ");
        query.append(String.join(", ", keywords));
        
        if (!owners.isEmpty()) {
            query.append(" 소유자: ").append(String.join(", ", owners));
        }

        return query.toString();
    }

    /**
     * 리소스 식별자에서 키워드 추출
     */
    private List<String> extractKeywords(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return List.of();
        }

        // URL 경로에서 키워드 추출
        if (identifier.startsWith("/")) {
            return List.of(identifier.split("/"))
                    .stream()
                    .filter(part -> !part.isEmpty() && !part.matches("\\{.*\\}")) // 경로 변수 제외
                    .collect(Collectors.toList());
        }

        // 메서드명에서 키워드 추출
        if (identifier.contains(".")) {
            String[] parts = identifier.split("\\.");
            String methodName = parts[parts.length - 1].replace("()", "");
            
            // camelCase 분리
            String[] camelParts = methodName.split("(?=\\p{Upper})");
            return List.of(camelParts);
        }

        // 기본적으로 전체 식별자를 키워드로 사용
        return List.of(identifier);
    }

    /**
     * 검색된 문서들을 컨텍스트 문자열로 변환
     */
    private String buildContextFromDocuments(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        context.append("유사한 리소스 네이밍 사례들:\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append(i + 1).append(". ");
            
            // 메타데이터가 있으면 활용
            if (doc.getMetadata().containsKey("identifier")) {
                context.append("식별자: ").append(doc.getMetadata().get("identifier"));
            }
            if (doc.getMetadata().containsKey("friendlyName")) {
                context.append(" → 친화적 이름: ").append(doc.getMetadata().get("friendlyName"));
            }
            
            context.append("\n");
            
            // 문서 내용 추가 (너무 길면 자르기)
            String content = doc.getText();
            if (content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            context.append("   설명: ").append(content).append("\n\n");
        }

        return context.toString();
    }

    public String getRetrieverName() {
        return "resource-naming-context";
    }
} 