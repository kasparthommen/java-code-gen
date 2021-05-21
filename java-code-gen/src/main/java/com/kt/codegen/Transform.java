package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a concrete class transformation with a target class name, {@link #targetName()},
 * and a list of regular expression {@link #replacements()} applied to the source file.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Transform {
    /**
     * The simple (i.e., non-fully qualified and thus package-free) target class name to generate.
     */
    String targetName();

    /**
     * An array of regular expression string replacements to apply to the source class.
     */
    Replace[] replacements();
}