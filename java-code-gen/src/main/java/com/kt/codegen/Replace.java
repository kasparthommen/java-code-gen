package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a string replacement (plain or regex).
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
     * The string to replace it with.
     *
     * @return The string to replace it with.
     */
    String to();

    /**
     * Specifies if the string replacements should be plain (false) or using regular expressions (true).
     *
     * @return Specifies if the string replacements should be plain (false) or using regular expressions (true).
     */
    boolean regex() default false;
}
