package com.kt.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Template {
    String sourceDir() default "src/main/java";
    Class<?>[] types1() default {};
    Class<?>[] types2() default {};
}
