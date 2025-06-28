package io.spring.iam.aiam.protocol;

import io.spring.iam.aiam.protocol.enums.RequestPriority;

/**
 * AIAM 진단 요청 기본 인터페이스
 */
public interface AIAMRequest {
    RequestPriority getPriority();
} 