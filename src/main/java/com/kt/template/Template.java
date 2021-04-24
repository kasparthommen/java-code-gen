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
     * The source directory relative to the classpath where <code>.class</code> files are residing. Defaults
     * to Maven's <code>../../src/main/java</code>, i.e., up two levels from <code>target/classes/</code>
     * and then down to <code>src/main/java</code>
     */
    String relativeSourceDir() default "../../src/main/java";

    /**
     * If true the type names are being appended to the original class name. If false the type names are
     * being prepended to the original class name.
     */
    boolean appendTypeNames() default true;

    /**
     * An optional array of two entries the first one being a string to search in the generated class
     * name and the secona one its replacement.
     */
    String[] classNameReplacement() default {};

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
