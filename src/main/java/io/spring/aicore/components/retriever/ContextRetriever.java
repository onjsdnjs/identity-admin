package io.spring.aicore.components.retriever;

import io.spring.aicore.protocol.AIRequest;
import io.spring.aicore.protocol.DomainContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 기반 컨텍스트 검색기
 * 
 * 🔍 현재 하드코딩된 Vector DB 검색 로직을 체계화
 * - 자연어 쿼리 기반 관련 문서 검색
 * - 검색 결과 정제 및 컨텍스트 구성
 * - 다양한 검색 전략 지원
 */
@Component
public class ContextRetriever {
    
    private final VectorStore vectorStore;
    
    public ContextRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    /**
     * 자연어 쿼리를 기반으로 관련 컨텍스트를 검색합니다
     * 
     * @param request AI 요청 (자연어 쿼리 포함)
     * @return 검색된 컨텍스트 정보
     */
    public ContextRetrievalResult retrieveContext(AIRequest<? extends DomainContext> request) {
        String query = extractQueryFromRequest(request);
        
        // 1. Vector DB 검색 (현재 하드코딩된 로직과 동일)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(10)
                .build();
                
        List<Document> contextDocs = vectorStore.similaritySearch(searchRequest);
        
        // 2. 검색 결과 정제 (현재 하드코딩된 로직과 동일)
        String contextInfo = contextDocs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n"));
                
        // 3. 메타데이터 수집
        Map<String, Object> metadata = Map.of(
            "documentsFound", contextDocs.size(),
            "searchQuery", query,
            "retrievalTime", System.currentTimeMillis()
        );
        
        return new ContextRetrievalResult(contextInfo, contextDocs, metadata);
    }
    
    /**
     * 요청에서 검색 쿼리를 추출합니다
     */
    private String extractQueryFromRequest(AIRequest<? extends DomainContext> request) {
        // 현재는 간단하게 구현, 나중에 요청 타입별로 확장 가능
        return request.toString(); // 실제로는 요청에서 자연어 쿼리 추출
    }
    
    /**
     * 컨텍스트 검색 결과
     */
    public static class ContextRetrievalResult {
        private final String contextInfo;
        private final List<Document> documents;
        private final Map<String, Object> metadata;
        
        public ContextRetrievalResult(String contextInfo, List<Document> documents, Map<String, Object> metadata) {
            this.contextInfo = contextInfo;
            this.documents = documents;
            this.metadata = metadata;
        }
        
        public String getContextInfo() { return contextInfo; }
        public List<Document> getDocuments() { return documents; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
} 