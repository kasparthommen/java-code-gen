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

import static com.kt.codegen.CodeGenerationHelper.findSourceDirectory;
import static com.kt.codegen.CodeGenerationHelper.readSourceCode;
import static com.kt.codegen.CodeGenerationHelper.removeAnnotations;
import static com.kt.codegen.CodeGenerationHelper.removeImport;
import static com.kt.codegen.CodeGenerationHelper.replace;
import static com.kt.codegen.CodeGenerationHelper.writeFile;
import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


/**
 * The annotation processor for {@link Transform} and {@link Instantiate} annotations.
 */
@SupportedAnnotationTypes({
        "com.kt.codegen.Transforms",
        "com.kt.codegen.Transform",
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
            for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(Transforms.class, Transform.class))) {
                processTransform((TypeElement) element, messager);
            }
            for (Element element : roundEnv.getElementsAnnotatedWithAny(Set.of(Instantiations.class, Instantiate.class))) {
                processInstantiations((TypeElement) element, messager);
            }
        } catch (CodeGeneratorException ex) {
            messager.printMessage(ERROR, ex.getMessage());
        }

        return true;
    }

    private void processTransform(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Creating code transform for " + sourceClass.getQualifiedName());
        Transforms transforms = sourceClass.getAnnotation(Transforms.class);
        if (transforms != null) {
            for (Transform transform : transforms.value()) {
                processTransform(sourceClass, transform, messager);
            }
        }
        Transform transform = sourceClass.getAnnotation(Transform.class);
        if (transform != null) {
            processTransform(sourceClass, transform, messager);
        }
    }

    private void processTransform(TypeElement sourceClass, Transform transform, Messager messager) {
        process(
                sourceClass,
                getSourceDirectory(sourceClass),
                new Class[] { Transform.class, Transforms.class, SourceDirectory.class },
                transform,
                messager);
    }

    private void processInstantiations(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Creating instantiations for generic class " + sourceClass.getQualifiedName());
        Instantiations instantiations = sourceClass.getAnnotation(Instantiations.class);
        if (instantiations != null) {
            for (Instantiate instantiation : instantiations.value()) {
                processInstantiation(sourceClass, instantiation, messager);
            }
        }
        Instantiate instantiation = sourceClass.getAnnotation(Instantiate.class);
        if (instantiation != null) {
            processInstantiation(sourceClass, instantiation, messager);
        }
    }

    private void processInstantiation(TypeElement sourceClass, Instantiate instantiation, Messager messager) {
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

        TransformImpl transform = new TransformImpl(targetClassName, customAndTypeReplaces.toArray(Replace[]::new));

        process(
                sourceClass,
                getSourceDirectory(sourceClass),
                new Class[] { Instantiate.class, Instantiations.class, SourceDirectory.class },
                transform,
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
            Transform transform,
            Messager messager) {
        // read source file
        Path sourceDir = findSourceDirectory(relativeSourceDir, messager);
        String sourceCode = readSourceCode(sourceDir, sourceClass, messager);
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();

        // generate target files
        String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
        String fullyQualifiedTargetClassName = pkg + "." + transform.target();
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
                transform.replace());

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

        targetCode = removeImport(targetCode, Transforms.class.getName(), fullyQualifiedSourceClassName);
        targetCode = removeImport(targetCode, Transform.class.getName(), fullyQualifiedSourceClassName);
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

    private static class TransformImpl implements Transform {
        private final String target;
        private final Replace[] replaces;

        private TransformImpl(String target, Replace[] replaces) {
            this.target = target;
            this.replaces = replaces;
        }

        @Override
        public String target() {
            return target;
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
