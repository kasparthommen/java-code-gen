package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Container annotation for {@link Generate}, see {@link java.lang.annotation.Repeatable},
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Generates {
    Generate[] value();
}
