package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies that a new class should be generated/derived off the annotated class. The target class
 * name is specified by {@link #name()} and string replacements (plain or regex) are specified by
 * {@link #replace()}.
 */
@Repeatable(Derivatives.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Derive {
    /**
     * The simple (i.e., package-stripped) target class name to generate.
     *
     * @return The simple (i.e., package-stripped) target class name to generate.
     */
    String name();

    /**
     * An optional list of string replacements (plain or regex) to apply to the source code.
     * This can be useful to e.g. replace generic array construction of generic type {@code T1} with
     * primitive array construction:
     * <br><br>
     *
     * {@code @Replace(from = "(T1[]) new Object[", to = "new double[" }
     *
     * @return An optional list of string replacements (plain or regex) to apply to the source code..
     */
    Replace[] replace() default {};
}
