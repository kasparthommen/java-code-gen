package com.kt.template;


import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


@SupportedAnnotationTypes("com.kt.template.CodeTransformer")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class CodeTransformerProcessor extends AbstractProcessor {
    private static final Function<String, String> FQ_TO_CLASS = s -> s.substring(s.lastIndexOf('.') + 1);
    private static final Function<String, String> FQ_TO_PACKAGE = s -> s.substring(0, s.lastIndexOf('.'));
    private static final Function<String, String> FIRST_UPPER = s -> s.substring(0, 1).toUpperCase() + s.substring(1);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();

        for (Element element : roundEnv.getElementsAnnotatedWith(CodeTransformer.class)) {
            TypeElement sourceClass = (TypeElement) element;
            messager.printMessage(NOTE, "Creating class replacements for " + sourceClass.getQualifiedName());
            CodeTransformer transformer = sourceClass.getAnnotation(CodeTransformer.class);

            // find source file
            Path classFileDir;
            try {
                classFileDir = Path.of(getClass().getClassLoader().getResource(".").toURI());
            } catch (URISyntaxException ex) {
                messager.printMessage(ERROR, ex.getMessage());
                return true;
            }
            Path sourceDir = classFileDir.resolve(transformer.relativeSourceDir()).normalize();
            if (!Files.exists(sourceDir)) {
                messager.printMessage(ERROR, "Source path not found: " + sourceDir
                        + ". Possibly a mis-specification of the relative source directory (" + transformer.relativeSourceDir() + ")?");
                return true;
            }
            messager.printMessage(NOTE, "sourceDir=" + sourceDir);
            String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();
            String relativePath = fullyQualifiedSourceClassName.replace(".", File.separator) + ".java";
            Path sourceFile = sourceDir.resolve(relativePath);
            if (!Files.exists(sourceFile)) {
                messager.printMessage(ERROR, "Source file not found: " + sourceFile);
            }
            messager.printMessage(NOTE, "sourceFile=" + sourceFile);

            // read source code
            String source;
            try {
                source = Files.readString(sourceFile).replace("\r", "");
            } catch (IOException ex) {
                messager.printMessage(ERROR, toString(ex));
                return true;
            }

            // read replacements and only keep the non-default ones
            Predicate<Transform> isNonDefault = transform -> !"".equals(transform.targetName());
            List<Transform> transforms = List.of(
                    transformer.t1(),
                    transformer.t2(),
                    transformer.t3(),
                    transformer.t4(),
                    transformer.t5(),
                    transformer.t6(),
                    transformer.t7(),
                    transformer.t8(),
                    transformer.t9(),
                    transformer.t10()
            ).stream().filter(isNonDefault).toList();

            // make sure that replacement arrays are consistent
            if (transforms.size() == 0) {
                messager.printMessage(ERROR, "No replacements supplied");
                return true;
            }

            // generate target files
            String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
            String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
            for (int i = 0; i < transforms.size(); i++) {
                Transform transform = transforms.get(i);

                String fullyQualifiedTargetClassName = pkg + "." + transform.targetName();
                if (fullyQualifiedTargetClassName.equals(fullyQualifiedSourceClassName)) {
                    messager.printMessage(ERROR, "Target class name must be different from source class name, but was "
                            + fullyQualifiedTargetClassName);
                    return true;
                }
                messager.printMessage(NOTE, "Creating " + fullyQualifiedTargetClassName + " from " + sourceClassName);
                String targetCode = generateTarget(
                        source,
                        fullyQualifiedSourceClassName,
                        fullyQualifiedTargetClassName,
                        transform.replacements());

                writeFile(targetCode, fullyQualifiedTargetClassName, messager);
            }
        }

        return true;
    }

    private static String generateTarget(
            String source,
            String fullyQualifiedSourceClassName,
            String fullyQualifiedTargetClassName,
            Replace[] replacements) {
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String targetClassName = FQ_TO_CLASS.apply(fullyQualifiedTargetClassName);

        String target = source;

        // remove imports
        target = removeImport(target, CodeTransformer.class.getName());
        target = removeImport(target, Transform.class.getName());
        target = removeImport(target, Replace.class.getName());

        // remove @CodeTransformer(...) annotation
        String annotation = "@" + CodeTransformer.class.getSimpleName();
        int annotationStartIndex = target.indexOf(annotation);
        target = target.replace(annotation, "");
        int parenthesisCount = 0;
        int annotationEndIndex = annotationStartIndex;
        boolean inString = false;
        charLoop:
        while (true) {
            if (annotationEndIndex >= target.length()) {
                throw new IllegalStateException("Internal error: Reached end target source file but didn't find end of annotation");
            }
            char c = target.charAt(annotationEndIndex);

            if (inString) {
                // handle escapes when walking through strings
                switch (c) {
                    case '\\':
                        // escaped character -> skip over next
                        annotationEndIndex++;
                        break;
                    case '\"':
                        // end of string
                        inString = false;
                        break;
                }
            } else {
                // outside string
                switch (c) {
                    case '\"':
                        inString = true;
                        break;
                    case '(':
                        parenthesisCount++;
                        break;
                    case ')':
                        parenthesisCount--;
                        if (parenthesisCount == 0) {
                            target = target.substring(0, annotationStartIndex)
                                    + "// generated from $$$$"
                                    + target.substring(annotationEndIndex + 1);
                            break charLoop;
                        }
                }
            }
            annotationEndIndex++;
        }

        // process custom replacements
        for (Replace replacement : replacements) {
            Pattern pattern = Pattern.compile(replacement.from(), Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(target);
            if (matcher.find()) {
                target = matcher.replaceAll(replacement.to());
            }
        }

        // replace class declaration
        target = target.replaceAll("\\b" + sourceClassName + "\\b", targetClassName);

        // complete replacement comment
        target = target.replace("$$$$", fullyQualifiedSourceClassName);

        return target;
    }

    private static String removeImport(String target, String fullyQualifiedClassName) {
        String importStatement = "\nimport " + fullyQualifiedClassName + ";\n";
        for (int emptyLinesBefore = 2; emptyLinesBefore >= 0; emptyLinesBefore--) {
            String before = "\n".repeat(emptyLinesBefore);
            for (int emptyLinesAfter = 2; emptyLinesAfter >= 0; emptyLinesAfter--) {
                String after = "\n".repeat(emptyLinesAfter);
                String emptyLines = "\n".repeat(1 + Math.max(emptyLinesBefore, emptyLinesAfter));
                target = target.replace(before + importStatement + after, emptyLines);
            }
        }
        return target;
    }

    private void writeFile(String source, String fullyQualifiedTargetClassName, Messager messager) {
        try {
            JavaFileObject targetFile = processingEnv.getFiler().createSourceFile(fullyQualifiedTargetClassName);
            try (PrintWriter targetWriter = new PrintWriter(targetFile.openWriter())) {
                targetWriter.write(source);
            }
        } catch (IOException ex) {
            messager.printMessage(ERROR, "Could not generate file " + fullyQualifiedTargetClassName + ": " + ex.getMessage());
        }
    }

    private static String replaceToken(String s, String from, String to) {
        return s.replaceAll("\\b" + from + "\\b", to);
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
