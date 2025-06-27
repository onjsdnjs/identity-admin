package io.spring.iam.resource.service;

/**
 * 메서드 시그니처 정보
 * ✅ SRP 준수: 메서드 시그니처 정보만 담당
 */
public class MethodSignature {
    public final String methodName;
    public final String parameterInfo;
    public final String resourceType;
    
    public MethodSignature(String methodName, String parameterInfo, String resourceType) {
        this.methodName = methodName;
        this.parameterInfo = parameterInfo;
        this.resourceType = resourceType;
    }
} 