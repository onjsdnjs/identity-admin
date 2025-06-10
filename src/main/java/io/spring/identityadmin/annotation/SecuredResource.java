package io.spring.identityadmin.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredResource {
    String name();
    String description() default "";
}
