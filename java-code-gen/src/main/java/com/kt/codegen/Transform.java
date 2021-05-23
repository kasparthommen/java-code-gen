package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a concrete class transformation with a target class name, {@link #target()},
 * and a list of regular expression {@link #replace()} applied to the source file.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Transform {
    /**
     * The simple (i.e., package-stripped) target class name to generate.
     *
     * @return The simple (i.e., package-stripped) target class name to generate.
     */
    String target();

    /**
     * An optional list of string replacements (plain or regex) to apply on top of the generic type replacements.
     * This can be useful to e.g. replace generic array construction of generic type {@code T1} with
     * primitive array construction:
     * <br><br>
     *
     * {@code @Replace(from = "(T1[]) new Object[", to = "new double[" }
     *
     * @return An optional list of string replacements (plain or regex) to apply on top of the generic type replacements.
     */
    Replace[] replace();
}
