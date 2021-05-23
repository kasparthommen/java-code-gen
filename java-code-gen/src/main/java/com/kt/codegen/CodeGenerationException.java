package com.kt.codegen;


/**
 * An exception type that represents errors during code generation.
 */
public class CodeGenerationException extends Exception {
    /**
     * Constructor.
     *
     * @param message The error message.
     */
    public CodeGenerationException(String message) {
        super(message);
    }
}
