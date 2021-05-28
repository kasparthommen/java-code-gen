package com.kt.codegen;


import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


/**
 * The annotation processor for {@link Derive} and {@link Instantiate} annotations.
 */
@SupportedAnnotationTypes({
        "com.kt.codegen.Derivatives",
        "com.kt.codegen.Derive",
        "com.kt.codegen.Instantiations",
        "com.kt.codegen.Instantiate"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class CodeGeneratorProcessor extends AbstractProcessor {
    static final String DEFAULT_RELATIVE_SRC_DIR = "../../src/main/java";

    private static final Function<String, String> FQ_TO_CLASS = s -> s.substring(s.lastIndexOf('.') + 1);
    private static final Function<String, String> FQ_TO_PACKAGE = s -> s.substring(0, s.lastIndexOf('.'));
    private static final Function<String, String> FIRST_UPPER = s -> s.substring(0, 1).toUpperCase() + s.substring(1);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        try {
            for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(Derivatives.class, Derive.class))) {
                processDerive((TypeElement) element, messager);
            }
            for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(Instantiations.class, Instantiate.class))) {
                processInstantiate((TypeElement) element, messager);
            }
        } catch (CodeGeneratorException ex) {
            messager.printMessage(ERROR, ex.getMessage());
        }

        return true;
    }

    private void processDerive(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Generating code for " + sourceClass.getQualifiedName());
        Derivatives derivatives = sourceClass.getAnnotation(Derivatives.class);
        if (derivatives != null) {
            for (Derive derive : derivatives.value()) {
                processDerive(sourceClass, derive, messager);
            }
        }
        Derive derive = sourceClass.getAnnotation(Derive.class);
        if (derive != null) {
            processDerive(sourceClass, derive, messager);
        }
    }

    private void processDerive(TypeElement sourceClass, Derive derive, Messager messager) {
        process(
                sourceClass,
                getSourceDirectory(sourceClass),
                new Class[] { Derive.class, Derivatives.class, SourceDirectory.class },
                derive,
                messager);
    }

    private void processInstantiate(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Creating instantiations for generic class " + sourceClass.getQualifiedName());
        Instantiations instantiations = sourceClass.getAnnotation(Instantiations.class);
        if (instantiations != null) {
            for (Instantiate instantiation : instantiations.value()) {
                processInstantiate(sourceClass, instantiation, messager);
            }
        }
        Instantiate instantiation = sourceClass.getAnnotation(Instantiate.class);
        if (instantiation != null) {
            processInstantiate(sourceClass, instantiation, messager);
        }
    }

    private void processInstantiate(TypeElement sourceClass, Instantiate instantiation, Messager messager) {
        String sourceClassName = sourceClass.getSimpleName().toString();
        TypeParameterElement[] typeParameters = sourceClass.getTypeParameters().toArray(TypeParameterElement[]::new);
        String[] typeParameterNames = Arrays.stream(typeParameters).map(Object::toString).toArray(String[]::new);

        TypeMirror[] concreteTypes = getTypes(instantiation);
        String[] concreteTypeNames = Stream.of(concreteTypes)
                                           .map(TypeMirror::toString)
                                           .map(FQ_TO_CLASS)
                                           .toArray(String[]::new);
        if (concreteTypeNames.length != typeParameters.length) {
            throw new CodeGeneratorException("Expected " + typeParameters.length + " type parameters, got " + Arrays.toString(concreteTypes));
        }

        messager.printMessage(NOTE, "Instantiating " + sourceClass.getQualifiedName() + " for " + Arrays.toString(concreteTypeNames));
        String typeNames = Arrays.stream(concreteTypeNames)
                                 .map(FIRST_UPPER)
                                 .collect(joining(""));

        String targetClassName = instantiation.append()
                ? sourceClassName + typeNames
                : typeNames + sourceClassName;

        List<Replace> customAndTypeReplaces = new ArrayList<>(Arrays.asList(instantiation.replace()));
        customAndTypeReplaces.add(new ReplaceImpl(sourceClassName + "\\s*<[\\s\\w\\?,]+>\\s*\\{", targetClassName + " {", true));
        for (int i = 0; i < typeParameterNames.length; i++) {
            String from = "\\b" + typeParameterNames[i] + "\\b";
            String to = concreteTypeNames[i];
            customAndTypeReplaces.add(new ReplaceImpl(from, to, true));
        }

        DeriveImpl derive = new DeriveImpl(targetClassName, customAndTypeReplaces.toArray(Replace[]::new));

        process(
                sourceClass,
                getSourceDirectory(sourceClass),
                new Class[] { Instantiate.class, Instantiations.class, SourceDirectory.class },
                derive,
                messager);
        }

    private String getSourceDirectory(TypeElement sourceClass) {
        return sourceClass.getAnnotation(SourceDirectory.class) != null
                ? sourceClass.getAnnotation(SourceDirectory.class).value()
                : DEFAULT_RELATIVE_SRC_DIR;
    }

    private void process(
            TypeElement sourceClass,
            String relativeSourceDir,
            Class<? extends Annotation>[] annotationTypes,
            Derive derive,
            Messager messager) {
        // read source file
        Path sourceDir = findSourceDirectory(relativeSourceDir, messager);
        String sourceCode = readSourceCode(sourceDir, sourceClass, messager);
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();

        // generate target files
        String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
        String fullyQualifiedTargetClassName = pkg + "." + derive.name();
        if (fullyQualifiedTargetClassName.equals(fullyQualifiedSourceClassName)) {
            throw new CodeGeneratorException(
                    "Target class name must be different from source class name, but was " + fullyQualifiedTargetClassName);
        }
        messager.printMessage(NOTE, "Creating " + fullyQualifiedTargetClassName + " from " + fullyQualifiedSourceClassName);
        String targetCode = generateTarget(
                fullyQualifiedSourceClassName,
                fullyQualifiedTargetClassName,
                annotationTypes,
                sourceCode,
                derive.replace());

        writeFile(targetCode, fullyQualifiedTargetClassName, processingEnv);
    }

    private static String generateTarget(
            String fullyQualifiedSourceClassName,
            String fullyQualifiedTargetClassName,
            Class<? extends Annotation>[] annotationTypes,
            String sourceCode,
            Replace[] replacements) {
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String targetClassName = FQ_TO_CLASS.apply(fullyQualifiedTargetClassName);

        String targetCode = sourceCode;

        targetCode = replace(replacements, targetCode, fullyQualifiedSourceClassName);

        targetCode = removeImport(targetCode, Derivatives.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, Derive.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, Instantiations.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, Instantiate.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, Replace.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, SourceDirectory.class.getName(), fullyQualifiedSourceClassName);

        targetCode = removeAnnotations(targetCode, annotationTypes, fullyQualifiedSourceClassName);

        targetCode = targetCode.replaceAll("\\b" + sourceClassName + "\\b", targetClassName);
        targetCode = "// generated from " + fullyQualifiedSourceClassName + "\n" + targetCode;

        return targetCode;
    }

    private static  TypeMirror[] getTypes(Instantiate instantiations) {
        // uses this trick:
        // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation/52793839#52793839
        try {
            instantiations.value();
        } catch (MirroredTypesException mtex) {
            List<? extends TypeMirror> typeMirrors = mtex.getTypeMirrors();
            return typeMirrors.isEmpty() ? null : typeMirrors.toArray(TypeMirror[]::new);
        }
        throw new IllegalStateException("Cannot get to here");
    }

    private static Path findSourceDirectory(String relativeSourceDir, Messager messager) {
        Path classFileDir;
        try {
            classFileDir = Path.of(CodeGeneratorProcessor.class.getClassLoader().getResource(".").toURI());
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

    private static String readSourceCode(Path sourceDir, TypeElement sourceClass, Messager messager) {
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

    private static String removeImport(String code, String fullyQualifiedClassName, String fullyQualifiedSourceClassName) {
        return replace(
                code,
                "^\\s*import\\s+" + fullyQualifiedClassName + "\\s*;\\s*\n", "\n",
                ReplacementMethod.REGEX_ALL,
                false,
                fullyQualifiedSourceClassName);
    }

    private static String removeAnnotations(
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

    private static String replace(Replace[] replacements, String code, String fullyQualifiedSourceClassName) {
        for (Replace replacement : replacements) {
            String from = replacement.from();
            String to = replacement.to();
            ReplacementMethod replacementMethod = replacement.regex() ? ReplacementMethod.REGEX_ALL : ReplacementMethod.PLAIN_ALL;
            code = replace(code, from, to, replacementMethod, true, fullyQualifiedSourceClassName);
        }
        return code;
    }

    private static String replace(
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

    private static void writeFile(
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

    private enum ReplacementMethod {
        PLAIN_ALL,
        REGEX_ALL,
        REGEX_FIRST
    }

    private static class DeriveImpl implements Derive {
        private final String name;
        private final Replace[] replaces;

        private DeriveImpl(String name, Replace[] replaces) {
            this.name = name;
            this.replaces = replaces;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Replace[] replace() {
            return replaces;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ReplaceImpl implements Replace {
        private final String from;
        private final String to;
        private final boolean regex;

        private ReplaceImpl(String from, String to, boolean regex) {
            this.from = from;
            this.to = to;
            this.regex = regex;
        }

        @Override
        public String from() {
            return from;
        }

        @Override
        public String to() {
            return to;
        }

        @Override
        public boolean regex() {
            return regex;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            throw new UnsupportedOperationException();
        }
    }
}
