package com.kt.codegen;


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
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.kt.codegen.CodeGeneratorHelper.findSourceDirectory;
import static com.kt.codegen.CodeGeneratorHelper.readSourceCode;
import static com.kt.codegen.CodeGeneratorHelper.removeAnnotations;
import static com.kt.codegen.CodeGeneratorHelper.removeImport;
import static com.kt.codegen.CodeGeneratorHelper.replace;
import static com.kt.codegen.CodeGeneratorHelper.writeFile;
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
