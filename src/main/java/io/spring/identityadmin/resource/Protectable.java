package io.spring.identityadmin.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [재설계]
 * 동적 인가 정책의 대상이 되는 서비스 계층의 메서드를 명시적으로 지정하고,
 * 해당 메서드에 필요한 '비즈니스 권한 이름'을 선언합니다.
 * MethodResourceScanner는 이 어노테이션이 붙은 메서드만 스캔합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Protectable {

    /**
     * 이 메서드를 보호하는, 시스템 내에서 고유한 비즈니스 권한의 이름.
     * 이 이름이 Permission 엔티티의 'name' 필드와 연결되는 키가 됩니다.
     * 예: "ORDER_APPROVE", "USER_DELETE"
     * @return 권한 이름
     */
    String name() default "";

    /**
     * 권한 관리자가 이 권한의 용도를 이해할 수 있도록 돕는 설명.
     * 이 설명은 '리소스 워크벤치'에 기본값으로 표시됩니다.
     * @return 권한 설명
     */
    String description() default "";
}