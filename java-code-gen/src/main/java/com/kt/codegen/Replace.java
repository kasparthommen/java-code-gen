package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a string or regular expression replacement in source code.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Replace {
    /**
     * The plain string or regex expression to replace.
     *
     * @return The plain string or regex expression to replace.
     */
    String from();

    /**
     * The string to replace {@link #from()} with.
     *
     * @return The string to replace {@link #from()} with.
     */
    String to();

    /**
     * Specifies if {@link #from()} is a plain string to replace (false) or a regular
     * expression (true) to replace.
     *
     * @return Specifies if {@link #from()} is a plain string to replace (false) or a
     * regular expression (true) to replace.
     */
    boolean regex() default false;
}
