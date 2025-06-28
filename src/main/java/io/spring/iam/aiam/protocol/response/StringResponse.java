package io.spring.iam.aiam.protocol.response;

import io.spring.iam.aiam.protocol.IAMResponse;
import io.spring.aicore.protocol.AIResponse.ExecutionStatus;

/**
 * 모든 Strategy에서 공용으로 사용할 String 응답 클래스
 * 
 * ✅ AINativeIAMOperations 수정 없이 타입 통일을 위한 공용 응답
 */
public class StringResponse extends IAMResponse {
    private final String content;
    
    public StringResponse(String requestId, String content) {
        super(requestId, ExecutionStatus.SUCCESS);
        this.content = content;
    }
    
    public StringResponse(String requestId, ExecutionStatus status, String content) {
        super(requestId, status);
        this.content = content;
    }
    
    @Override
    public Object getData() {
        return content;
    }
    
    @Override
    public String getResponseType() {
        return "STRING_RESPONSE";
    }
    
    public String getContent() {
        return content;
    }
    
    @Override
    public String toString() {
        return String.format("StringResponse{requestId='%s', status='%s', contentLength=%d}",
                getResponseId(), getStatus(), content != null ? content.length() : 0);
    }
} 