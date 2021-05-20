package com.kt.template;


/**
 * Specifies a regular expression string replacement.
 */
public @interface Replace {
    /**
     * The regular expression to replace.
     */
    String from();

    /**
     * The string to replace it with.
     */
    String to();
}
