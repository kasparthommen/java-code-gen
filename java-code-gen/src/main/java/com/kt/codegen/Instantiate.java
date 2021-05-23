package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a concrete type-specific instantiation of the generic class,
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Instantiate {
    /**
     * The concrete types to replace the type parameters with. The number of concrete types must match
     * the number of type arguments.
     *
     * @return The concrete types to replace the type parameters with.
     */
    Class<?>[] value();

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
    Replace[] replace() default {};
}
