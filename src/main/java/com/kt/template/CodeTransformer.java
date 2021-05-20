package com.kt.template;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * A source code generator that generates new classes by performing regular expression string
 * replacements on the source class file contents.
 * <p/>
 *
 * Currently up to ten classes derived from the annotated class can be generated using the
 * transformations ({@link #t1} through {@link #t10}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CodeTransformer {
    /**
     * The source directory relative to the classpath where <code>.class</code> files are residing. Defaults
     * to Maven's <code>../../src/main/java</code>, i.e., up two levels from <code>target/classes/</code>
     * and then down to <code>src/main/java</code>
     */
    String relativeSourceDir() default "../../src/main/java";

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t1() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t2() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t3() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t4() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t5() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t6() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t7() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t8() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t9() default @Transform(targetName = "", replacements = {});

    /** A target class code generation setup, see {@link CodeTransformer}. */
    Transform t10() default @Transform(targetName = "", replacements = {});
}
