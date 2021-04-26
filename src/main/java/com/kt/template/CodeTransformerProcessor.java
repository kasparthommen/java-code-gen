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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

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
            CodeTransformer template = sourceClass.getAnnotation(CodeTransformer.class);

            // find source file
            Path classFileDir;
            try {
                classFileDir = Path.of(getClass().getClassLoader().getResource(".").toURI());
            } catch (URISyntaxException ex) {
                messager.printMessage(ERROR, ex.getMessage());
                return true;
            }
            Path sourceDir = classFileDir.resolve(template.relativeSourceDir()).normalize();
            if (!Files.exists(sourceDir)) {
                messager.printMessage(ERROR, "Source path not found: " + sourceDir
                        + ". Possibly a mis-specification of the relative source directory (" + template.relativeSourceDir() + ")?");
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

            // read replacements
            List<String[]> replacements = new ArrayList<>();
            Consumer<String[]> toListAdderIfNotEmpty = rep -> {
                if (rep.length > 0) {
                    replacements.add(rep);
                }
            };
            toListAdderIfNotEmpty.accept(template.r1());
            toListAdderIfNotEmpty.accept(template.r2());
            toListAdderIfNotEmpty.accept(template.r3());
            toListAdderIfNotEmpty.accept(template.r4());
            toListAdderIfNotEmpty.accept(template.r5());
            toListAdderIfNotEmpty.accept(template.r6());
            toListAdderIfNotEmpty.accept(template.r7());
            toListAdderIfNotEmpty.accept(template.r8());
            toListAdderIfNotEmpty.accept(template.r9());
            toListAdderIfNotEmpty.accept(template.r10());

            // make sure that replacement arrays are consistent
            if (replacements.size() == 0) {
                messager.printMessage(ERROR, "No replacements supplied");
                return true;
            }
            for (int i = 0; i < replacements.size(); i++) {
                String[] rep = replacements.get(i);
                if (rep.length % 2 != 1) {
                    messager.printMessage(ERROR, "Replacement array must contain an odd number of elements: class name plus an " +
                            "arbitrary number of <from> <to> tuples, but got " + Arrays.toString(rep));
                    return true;
                }
            }

            // generate target files
            String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
            String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
            for (int i = 0; i < replacements.size(); i++) {
                String[] rep = replacements.get(i);
                String targetClassName = rep[0];
                String[] froms = IntStream.range(1, rep.length).filter(idx -> idx % 2 == 1).mapToObj(idx -> rep[idx]).toArray(String[]::new);
                String[] tos = IntStream.range(1, rep.length).filter(idx -> idx % 2 == 0).mapToObj(idx -> rep[idx]).toArray(String[]::new);
                assert froms.length == tos.length;

                String fullyQualifiedTargetClassName = pkg + "." + targetClassName;
                messager.printMessage(NOTE, "Creating " + targetClassName + " from " + sourceClassName);
                String targetCode = generateTarget(
                        source,
                        fullyQualifiedSourceClassName,
                        fullyQualifiedTargetClassName,
                        froms,
                        tos);

                writeFile(targetCode, fullyQualifiedTargetClassName, messager);
            }
        }

        return true;
    }

    public static String generateTarget(
            String source,
            String fullyQualifiedSourceClassName,
            String fullyQualifiedTargetClassName,
            String[] froms,
            String[] tos) {
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String targetClassName = FQ_TO_CLASS.apply(fullyQualifiedTargetClassName);

        String target = source;

        // remove CodeTransformer import
        String importStatement = "\nimport " + CodeTransformer.class.getName() + ";\n";
        target = target.replace("\n\n" + importStatement + "\n\n", "\n\n\n");
        target = target.replace("\n\n" + importStatement + "\n", "\n\n\n");
        target = target.replace("\n" + importStatement + "\n\n", "\n\n\n");
        target = target.replace("\n" + importStatement + "\n", "\n\n");
        target = target.replace("\n" + importStatement, "\n\n");
        target = target.replace(importStatement + "\n", "\n\n");
        target = target.replace(importStatement, "\n");

        // process custom replacements
        for (int i = 0; i < froms.length; i++) {
            target = target.replaceAll(froms[i], tos[i]);
        }

        // replace class declaration
        target = target.replaceAll("\\b" + sourceClassName + "\\b", targetClassName);

        // remove @CodeTransformer(...) annotation
        String annotation = "@" + CodeTransformer.class.getSimpleName();
        int idx = target.indexOf(annotation);
        target = target.replace(annotation, "");
        int parenthesisCount = 0;
        int idx2 = idx;
        while (true) {
            char c = target.charAt(idx2);
            if (c == '(') {
                parenthesisCount++;
            } else if (c == ')') {
                parenthesisCount--;
                if (parenthesisCount == 0) {
                    target = target.substring(0, idx)
                            + "// generated from " + fullyQualifiedSourceClassName
                            + target.substring(idx2 + 1);
                    break;
                }
            }
            idx2++;
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
