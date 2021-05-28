package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies that a new class should be generated, derived off the annotated class. The target class
 * name is specified by {@link #name()} and string replacements (plain or regex) are specified by
 * {@link #replace()}.
 */
@Repeatable(Derivatives.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Derive {
    /**
     * The target class name (without package prefix) to generate.
     *
     * @return The target class name (without package prefix) to generate.
     */
    String name();

    /**
     * A list of string replacements (plain or regex) to apply to the source code. These replacements
     * specify how the source class should be modified to arrive at the derived target class. For
     * example, you may want to generate a derived version of a class that doesn't use {@code double}s
     * but {@code float}s instead. Then you could specify the following replacements:
     *
     * <pre>
     * {@code
     * replace = {
     *     @Replace(from = "\\bdouble\\b", to = "float", regex = true),
     *     @Replace(from = "Double.NaN", to = "Float.NaN"),
     *     ...
     * }
     * }
     * </pre>
     *
     * @return A list of string replacements (plain or regex) to apply to the source code..
     */
    Replace[] replace();
}
