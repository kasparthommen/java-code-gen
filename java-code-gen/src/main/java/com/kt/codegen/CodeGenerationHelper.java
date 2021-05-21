package com.kt.codegen;


import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.tools.Diagnostic.Kind.NOTE;


class CodeGenerationHelper {
    static final String SOURCE_CLASS_NAME_PLACEHOLDER = "${sourceClassName}";
    static final Function<String, String> FQ_TO_CLASS = s -> s.substring(s.lastIndexOf('.') + 1);
    static final Function<String, String> FQ_TO_PACKAGE = s -> s.substring(0, s.lastIndexOf('.'));
    static final Function<String, String> FIRST_UPPER = s -> s.substring(0, 1).toUpperCase() + s.substring(1);

    static Path findSourceDirectory(String relativeSourceDir, Messager messager) throws CodeGenerationException {
        Path classFileDir;
        try {
            classFileDir = Path.of(CodeGenerationHelper.class.getClassLoader().getResource(".").toURI());
        } catch (URISyntaxException ex) {
            throw new CodeGenerationException(ex.getMessage());
        }
        Path sourceDir = classFileDir.resolve(relativeSourceDir).normalize();
        if (sourceDir == null || !Files.exists(sourceDir)) {
            throw new CodeGenerationException("Source path not found: " + sourceDir
                    + ". Possibly a mis-specification of the relative source directory (" + relativeSourceDir + ")?");
        }
        messager.printMessage(NOTE, "sourceDir=" + sourceDir);
        return sourceDir;
    }

    static String readSourceCode(Path sourceDir, TypeElement sourceClass, Messager messager) throws CodeGenerationException {
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();
        String relativePath = fullyQualifiedSourceClassName.replace(".", File.separator) + ".java";
        Path sourceFile = sourceDir.resolve(relativePath);
        if (!Files.exists(sourceFile)) {
            throw new CodeGenerationException("Source file not found: " + sourceFile);
        }
        messager.printMessage(NOTE, "sourceFile=" + sourceFile);

        String source;
        try {
            source = Files.readString(sourceFile).replace("\r", "");
            return source;
        } catch (IOException ex) {
            throw new CodeGenerationException(toString(ex));
        }
    }

    static String removeImport(String code, String fullyQualifiedClassName) {
        return code.replaceAll("[ \\t]*import\\s+" + fullyQualifiedClassName + "\\s*;\\s*\n", "");
    }

    static String removeAnnotation(
            String code,
            Class<?> annotationType,
            String fullyQualifiedSourceClassName) throws CodeGenerationException {
        String annotationStartRegex = "@\\s*" + annotationType.getSimpleName() + "\\s*\\(";
        int annotationStartIndex = indexOfRegex(code, annotationStartRegex);
        if (annotationStartIndex == -1) {
            throw new CodeGenerationException("Annotation not found: " + annotationType);
        }

        code = replace(code, annotationStartRegex, "(", true, fullyQualifiedSourceClassName);
        code = skipBrackets('(', ')', code, annotationStartIndex);
        code = code.substring(0, annotationStartIndex) + code.substring(annotationStartIndex);
        if (code.substring(annotationStartIndex - 2).startsWith("\n\n\n")) {
            code = code.substring(0, annotationStartIndex) + code.substring(annotationStartIndex + 1);
        }
        return code;
    }

    static String skipBrackets(char opening, char closing, String code, int startIndex) {
        // there could be some whitespace before the opening bracket
        assert code.charAt(startIndex) == opening;
        int bracketCount = 0;
        int endIndex = startIndex;
        boolean inString = false;
        while (true) {
            if (endIndex >= code.length()) {
                throw new IllegalStateException("Internal error: Reached end of target code but didn't find end of annotation");
            }
            char c = code.charAt(endIndex);

            if (inString) {
                // handle escapes when walking through strings
                switch (c) {
                    case '\\':
                        // escaped character -> skip over next
                        endIndex++;
                        break;
                    case '\"':
                        // end of string
                        inString = false;
                        break;
                }
            } else {
                // outside string
                if (c == '\"') {
                    inString = true;
                } else if (c == opening) {
                    bracketCount++;
                } else if (c == closing) {
                    bracketCount--;
                    if (bracketCount == 0) {
                        return code.substring(0, startIndex) + code.substring(endIndex + 1);
                    }
                }
            }
            endIndex++;
        }
    }

    static String replace(Replace[] replacements, String code, String fullyQualifiedSourceClassName) throws CodeGenerationException {
        for (Replace replacement : replacements) {
            String from = replacement.from();
            String to = replacement.to();
            code = replace(code, from, to, replacement.regex(), fullyQualifiedSourceClassName);
        }
        return code;
    }

    static String replace(
            String code,
            String from,
            String to,
            boolean regex,
            String fullyQualifiedSourceClassName) throws CodeGenerationException {
        if (regex) {
            Pattern pattern = Pattern.compile(from, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                code = matcher.replaceAll(to);
            } else {
                throw new CodeGenerationException("Regex search term not found in " + fullyQualifiedSourceClassName + ": " + from);
            }
            return code;
        } else {
            if (code.contains(from)) {
                return code.replace(from, to);
            } else {
                throw new CodeGenerationException("Search term not found in " + fullyQualifiedSourceClassName + ": " + from);
            }
        }
    }

    static int indexOfRegex(String code, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.start() : -1;
    }

    private static String toString(Exception ex) {
        StackTraceElement[] trace = ex.getStackTrace();
        StringBuilder s = new StringBuilder();
        for (StackTraceElement traceElement : trace) {
            s.append("\tat ");
            s.append(traceElement);
            s.append("\n");
        }
        return s.toString();
    }

    static void writeFile(
            String source,
            String fullyQualifiedTargetClassName,
            ProcessingEnvironment processingEnv) throws CodeGenerationException {
        try {
            JavaFileObject targetFile = processingEnv.getFiler().createSourceFile(fullyQualifiedTargetClassName);
            try (PrintWriter targetWriter = new PrintWriter(targetFile.openWriter())) {
                targetWriter.write(source);
            }
        } catch (IOException ex) {
            throw new CodeGenerationException("Could not generate file " + fullyQualifiedTargetClassName + ": " + ex.getMessage());
        }
    }
}
