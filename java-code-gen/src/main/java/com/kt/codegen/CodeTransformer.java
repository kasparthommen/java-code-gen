package com.kt.codegen;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * A source code generator that generates new classes by performing string replacements
 * (optionally using regular expressions) on the source class file contents.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CodeTransformer {
    /**
     * The list of code transformation specifications.
     *
     * @return The list of code transformation specifications.
     */
    Transform[] value();

    /**
     * The source directory relative to the classpath where <code>.class</code> files are residing. Defaults
     * to Maven's <code>../../src/main/java</code>, i.e., up two levels from <code>target/classes/</code>
     * and then down to <code>src/main/java</code>
     *
     * @return The source directory relative to the classpath where <code>.class</code> files are residing.
     */
    String relativeSourceDir() default "../../src/main/java";
}
