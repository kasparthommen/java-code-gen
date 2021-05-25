package com.kt.codegen;


import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.tools.Diagnostic.Kind.NOTE;


class CodeGenerationHelper {
    enum ReplacementMethod {
        PLAIN_ALL,
        REGEX_ALL,
        REGEX_FIRST
    }

    static Path findSourceDirectory(String relativeSourceDir, Messager messager) {
        Path classFileDir;
        try {
            classFileDir = Path.of(CodeGenerationHelper.class.getClassLoader().getResource(".").toURI());
        } catch (URISyntaxException ex) {
            throw new CodeGeneratorException(ex.getMessage());
        }
        Path sourceDir = classFileDir.resolve(relativeSourceDir).normalize();
        if (sourceDir == null || !Files.exists(sourceDir)) {
            throw new CodeGeneratorException("Source path not found: " + sourceDir
                    + ". Possibly a mis-specification of the relative source directory (" + relativeSourceDir + ")?");
        }
        messager.printMessage(NOTE, "sourceDir=" + sourceDir);
        return sourceDir;
    }

    static String readSourceCode(Path sourceDir, TypeElement sourceClass, Messager messager) {
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();
        String relativePath = fullyQualifiedSourceClassName.replace(".", File.separator) + ".java";
        Path sourceFile = sourceDir.resolve(relativePath);
        if (!Files.exists(sourceFile)) {
            throw new CodeGeneratorException("Source file not found: " + sourceFile);
        }
        messager.printMessage(NOTE, "sourceFile=" + sourceFile);

        String source;
        try {
            source = Files.readString(sourceFile).replace("\r", "");
            return source;
        } catch (IOException ex) {
            throw new CodeGeneratorException(toString(ex));
        }
    }

    static String removeImport(String code, String fullyQualifiedClassName, String fullyQualifiedSourceClassName) {
        return replace(
                code,
                "^\\s*import\\s+" + fullyQualifiedClassName + "\\s*;\\s*\n", "\n",
                ReplacementMethod.REGEX_ALL,
                false,
                fullyQualifiedSourceClassName);
    }

    static String removeAnnotations(
            String code,
            Class<? extends Annotation>[] annotationTypes,
            String fullyQualifiedSourceClassName) {
        for (var annotationType : annotationTypes) {
            String annotationStartRegex = "@\\s*" + annotationType.getSimpleName() + "\\s*\\(";
            while (true) {
                int annotationStartIndex = indexOfRegex(code, annotationStartRegex);
                if (annotationStartIndex == -1) {
                    break;
                }
                code = replace(
                        code,
                        annotationStartRegex,
                        "(",
                        ReplacementMethod.REGEX_FIRST,
                        true,
                        fullyQualifiedSourceClassName);
                code = skipBrackets('(', ')', code, annotationStartIndex);
                code = code.substring(0, annotationStartIndex).replaceAll("\\s\\s+$", "\n\n") + code.substring(annotationStartIndex).replaceAll("^\\s*", "");
            }
        }
        return code;
    }

    static String replace(Replace[] replacements, String code, String fullyQualifiedSourceClassName) {
        for (Replace replacement : replacements) {
            String from = replacement.from();
            String to = replacement.to();
            ReplacementMethod replacementMethod = replacement.regex() ? ReplacementMethod.REGEX_ALL : ReplacementMethod.PLAIN_ALL;
            code = replace(code, from, to, replacementMethod, true, fullyQualifiedSourceClassName);
        }
        return code;
    }

    static String replace(
            String code,
            String from,
            String to,
            ReplacementMethod replacementMethod,
            boolean enforcePresence,
            String fullyQualifiedSourceClassName) {
        if (replacementMethod == ReplacementMethod.PLAIN_ALL) {
            if (code.contains(from)) {
                return code.replace(from, to);
            } else if (enforcePresence) {
                throw new CodeGeneratorException("Search term not found in " + fullyQualifiedSourceClassName + ": " + from);
            }
        } else {
            Pattern pattern = Pattern.compile(from, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                return replacementMethod == ReplacementMethod.REGEX_ALL
                        ? matcher.replaceAll(to)
                        : matcher.replaceFirst(to);
            } else if (enforcePresence) {
                throw new CodeGeneratorException("Regex search term not found in " + fullyQualifiedSourceClassName + ": " + from);
            }
        }
        return code;
    }

    static void writeFile(
            String source,
            String fullyQualifiedTargetClassName,
            ProcessingEnvironment processingEnv) {
        try {
            JavaFileObject targetFile = processingEnv.getFiler().createSourceFile(fullyQualifiedTargetClassName);
            try (PrintWriter targetWriter = new PrintWriter(targetFile.openWriter())) {
                targetWriter.write(source);
            }
        } catch (IOException ex) {
            throw new CodeGeneratorException("Could not generate file " + fullyQualifiedTargetClassName + ": " + ex.getMessage());
        }
    }

    private static String skipBrackets(char opening, char closing, String code, int startIndex) {
        if (code.charAt(startIndex) != opening) {
            throw new IllegalStateException("Internal error: opening bracket not where it's expected");
        }

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

    private static int indexOfRegex(String code, String regex) {
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
}
