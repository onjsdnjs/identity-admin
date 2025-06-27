package io.spring.iam.aiam.operations;

/**
 * IAM 운영 중 발생하는 예외를 처리하는 전용 예외 클래스
 */
public class IAMOperationException extends RuntimeException {
    
    public IAMOperationException(String message) {
        super(message);
    }
    
    public IAMOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 