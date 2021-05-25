package com.kt.codegen;


/**
 * An exception type that represents errors during code generation. Introduced to enable centralized
 * exception handling to send errors to the {@link javax.annotation.processing.Messager}.
 */
public class CodeGeneratorException extends RuntimeException {
    /**
     * Constructor.
     *
     * @param message The error message.
     */
    public CodeGeneratorException(String message) {
        super(message);
    }
}
