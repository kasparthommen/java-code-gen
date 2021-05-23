package com.kt.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * A C++-like template annotation. Creates concrete class instantiations replacing the generic type arguments
 * with the given concrete classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Template {
    /**
     * The list of desired concrete class instantiations.
     *
     * @return The list of desired concrete class instantiations.
     */
    Instantiate[] value();

    /**
     * The source directory relative to the classpath where <code>.class</code> files are residing. Defaults
     * to Maven's <code>../../src/main/java</code>, i.e., up two levels from <code>target/classes/</code>
     * and then down to <code>src/main/java</code>
     *
     * @return The source directory relative to the classpath where <code>.class</code> files are residing.
     */
    String relativeSourceDir() default "../../src/main/java";

    /**
     * Specifies if the concrete type names should be appended (true) or prepended (false) to the source class name.
     *
     * @return Specifies if the concrete type names should be appended (true) or prepended (false) to the source class name.
     */
    boolean append() default true;
}
