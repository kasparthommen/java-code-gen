package com.kt.template;

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
public @interface CodeTransformer {
    /**
     * The source directory relative to the classpath where <code>.class</code> files are residing. Defaults
     * to Maven's <code>../../src/main/java</code>, i.e., up two levels from <code>target/classes/</code>
     * and then down to <code>src/main/java</code>
     */
    String relativeSourceDir() default "../../src/main/java";

    String[] r1() default {};
    String[] r2() default {};
    String[] r3() default {};
    String[] r4() default {};
    String[] r5() default {};
    String[] r6() default {};
    String[] r7() default {};
    String[] r8() default {};
    String[] r9() default {};
    String[] r10() default {};
}
