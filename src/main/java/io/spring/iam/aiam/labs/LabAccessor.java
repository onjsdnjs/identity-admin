package io.spring.iam.aiam.labs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Lab 접근을 위한 완전 동적 헬퍼 클래스
 *
 * 🔧 완전 제네릭 기반 Lab 접근 시스템
 * - Lab 추가시 메서드 추가 불필요
 * - 타입 안전한 제네릭 접근
 * - 클래스 기반 동적 조회
 * - 클래스 이름 기반 동적 조회
 */
@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class LabAccessor {

    private final ApplicationContext applicationContext;
    private IAMLabRegistry labRegistry;

    /**
     * 타입으로 Lab을 조회합니다 (완전 제네릭)
     *
     * 사용 예:
     * Optional<ConditionTemplateGenerationLab> lab = labAccessor.getLab(ConditionTemplateGenerationLab.class);
     * Optional<AdvancedPolicyGenerationLab> lab = labAccessor.getLab(AdvancedPolicyGenerationLab.class);
     * Optional<AnyNewLab> lab = labAccessor.getLab(AnyNewLab.class);
     *
     * @param labType Lab 타입 클래스
     * @return Lab 인스턴스
     */
    public <T> Optional<T> getLab(Class<T> labType) {
        log.debug("🔍 Lab 조회 요청: {}", labType.getSimpleName());
        if(labRegistry == null){
            labRegistry = applicationContext.getBean(IAMLabRegistry.class);
        }
        return labRegistry.getLab(labType);
    }

    /**
     * 클래스 이름으로 Lab을 조회합니다 (동적 접근)
     *
     * 사용 예:
     * Optional<ConditionTemplateGenerationLab> lab = labAccessor.getLabByClassName("ConditionTemplateGenerationLab", ConditionTemplateGenerationLab.class);
     * Optional<Object> lab = labAccessor.getLabByClassName("ConditionTemplateGenerationLab");
     *
     * @param className 클래스 이름
     * @param expectedType 예상 타입 (타입 안전성을 위함)
     * @return Lab 인스턴스
     */
    public <T> Optional<T> getLabByClassName(String className, Class<T> expectedType) {
        log.debug("🔍 클래스 이름으로 Lab 조회: {} -> {}", className, expectedType.getSimpleName());
        return labRegistry.getLabByClassName(className)
                .filter(expectedType::isInstance)
                .map(expectedType::cast);
    }

    /**
     * 클래스 이름으로 Lab을 조회합니다 (타입 정보 없음)
     *
     * 사용 예:
     * Optional<Object> lab = labAccessor.getLabByClassName("ConditionTemplateGenerationLab");
     *
     * @param className 클래스 이름
     * @return Lab 인스턴스 (Object 타입)
     */
    public Optional<Object> getLabByClassName(String className) {
        log.debug("🔍 클래스 이름으로 Lab 조회 (타입 미지정): {}", className);
        return labRegistry.getLabByClassName(className);
    }

    /**
     * Lab 존재 여부를 확인합니다 (타입 기반)
     *
     * 사용 예:
     * boolean exists = labAccessor.hasLab(ConditionTemplateGenerationLab.class);
     *
     * @param labType Lab 타입
     * @return 존재 여부
     */
    public boolean hasLab(Class<?> labType) {
        log.debug("🔍 Lab 존재 여부 확인: {}", labType.getSimpleName());
        return labRegistry.isLabRegistered(labType);
    }

    /**
     * Lab 존재 여부를 클래스 이름으로 확인합니다
     *
     * 사용 예:
     * boolean exists = labAccessor.hasLab("ConditionTemplateGenerationLab");
     *
     * @param className 클래스 이름
     * @return 존재 여부
     */
    public boolean hasLab(String className) {
        log.debug("🔍 클래스 이름으로 Lab 존재 여부 확인: {}", className);
        return labRegistry.isLabRegistered(className);
    }

    /**
     * Lab을 안전하게 조회하고 실행합니다 (함수형 접근)
     *
     * 사용 예:
     * String result = labAccessor.withLab(ConditionTemplateGenerationLab.class,
     *     lab -> lab.generateUniversalConditionTemplates());
     *
     * @param labType Lab 타입
     * @param action Lab에서 실행할 액션
     * @return 액션 실행 결과
     */
    public <T, R> Optional<R> withLab(Class<T> labType, LabAction<T, R> action) {
        log.debug("🔧 Lab 함수형 실행: {}", labType.getSimpleName());

        return getLab(labType)
                .map(lab -> {
                    try {
                        return action.execute(lab);
                    } catch (Exception e) {
                        log.error("🔥 Lab 액션 실행 실패: {}", labType.getSimpleName(), e);
                        return null;
                    }
                });
    }

    /**
     * Lab 액션을 위한 함수형 인터페이스
     */
    @FunctionalInterface
    public interface LabAction<T, R> {
        R execute(T lab) throws Exception;
    }
} 