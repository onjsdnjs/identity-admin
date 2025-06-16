package io.spring.identityadmin.admin.monitoring.service;


import io.spring.identityadmin.admin.monitoring.dto.SecurityScoreDto;

/**
 * 시스템의 다양한 지표를 바탕으로 종합 보안 점수를 계산하는 책임을 가집니다.
 */
public interface SecurityScoreCalculator {
    SecurityScoreDto calculate();
}