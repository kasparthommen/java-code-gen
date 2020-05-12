package com.kt.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A C++-like template annotation. Creates concrete class instantiations replacing the generic type arguments
 * with the given concrete classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Template {
    /**
     * The source directory relative to the project root. Defaults to Maven's {@code src/main/java}.
     */
    String sourceDir() default "src/main/java";

    /**
     * The concrete classes to replace the 1st type argument with.
     */
    Class<?>[] types1() default {};

    /**
     * The concrete classes to replace the 2nd type argument with.
     */
    Class<?>[] types2() default {};
}
