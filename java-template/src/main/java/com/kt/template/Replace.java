package com.kt.template;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a regular expression string replacement.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Replace {
    /**
     * The regular expression to replace.
     */
    String from();

    /**
     * The string to replace it with.
     */
    String to();

    /**
     * The string replacement type, either plain or regex. Defaults to plain.
     */
    ReplaceType replaceType() default ReplaceType.PLAIN;
}
