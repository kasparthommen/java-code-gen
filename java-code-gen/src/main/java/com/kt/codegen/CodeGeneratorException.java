package com.kt.codegen;


/**
 * An exception type that represents errors during code generation in the annotation processor.
 * Introduced to enable centralized exception handling for sending error messages to the
 * {@link javax.annotation.processing.Messager}.
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
