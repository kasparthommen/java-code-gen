package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Allows the specification of a source directory relative to the class root directory
 * if the layout is different from the Maven default layout. If not set then the following
 * default is used: {@link CodeGeneratorProcessor#DEFAULT_RELATIVE_SRC_DIR}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SourceDirectory {
    /**
     * The source directory relative to the class root.
     *
     * @return The source directory relative to the class root.
     */
    String value();
}
