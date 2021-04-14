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
     * The concrete classes to replace the 1st type argument with, primitives types allowed.
     */
    Class<?>[] types1();

    /**
     * The concrete classes to replace the 2nd type argument (if present) with, primitives types allowed.
     */
    Class<?>[] types2() default {};

    /**
     * The concrete classes to replace the 3rd type argument (if present) with, primitives types allowed.
     */
    Class<?>[] types3() default {};

    /**
     * An optional array composed of concatenated triplets of the form [fullyQualifiedTypeName, from, to]
     * that represents custom string replacements that go beyond simple type replacements. For example,
     * the replacement triplet <code>{ "double", "(T1[]) new Object[", "new double[" }</code>
     * would be useful to convert generic type array construction to double array construction.
     */
    String[] replacements() default {};
}
