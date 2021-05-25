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
import java.util.stream.Stream;

import static com.kt.codegen.CodeGenerationHelper.FIRST_UPPER;
import static com.kt.codegen.CodeGenerationHelper.FQ_TO_CLASS;
import static com.kt.codegen.CodeGenerationHelper.FQ_TO_PACKAGE;
import static com.kt.codegen.CodeGenerationHelper.findSourceDirectory;
import static com.kt.codegen.CodeGenerationHelper.readSourceCode;
import static com.kt.codegen.CodeGenerationHelper.removeAnnotation;
import static com.kt.codegen.CodeGenerationHelper.removeImport;
import static com.kt.codegen.CodeGenerationHelper.replace;
import static com.kt.codegen.CodeGenerationHelper.writeFile;
import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


/**
 * The annotation processor for {@link CodeTransformer} and {@link Template} annotations.
 */
@SupportedAnnotationTypes({"com.kt.codegen.CodeTransformer", "com.kt.codegen.Template"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class CodeGeneratorProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        for (Element element : roundEnv.getElementsAnnotatedWith(CodeTransformer.class)) {
            try {
                processCodeTransformer((TypeElement) element, messager);
            } catch (CodeGeneratorException ex) {
                messager.printMessage(ERROR, ex.getMessage());
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Template.class)) {
            try {
                processTemplate((TypeElement) element, messager);
            } catch (CodeGeneratorException ex) {
                messager.printMessage(ERROR, ex.getMessage());
            }
        }

        return true;
    }

    private void processCodeTransformer(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Creating code transform for " + sourceClass.getQualifiedName());
        CodeTransformer codeTransformer = sourceClass.getAnnotation(CodeTransformer.class);
        Transform[] transforms = codeTransformer.value();
        process(
                sourceClass,
                codeTransformer.relativeSourceDir(),
                CodeTransformer.class,
                transforms,
                messager);
    }

    private void processTemplate(TypeElement sourceClass, Messager messager) {
        messager.printMessage(NOTE, "Creating instantiations for generic class " + sourceClass.getQualifiedName());
        Template template = sourceClass.getAnnotation(Template.class);
        String sourceClassName = sourceClass.getSimpleName().toString();
        TypeParameterElement[] typeParameters = sourceClass.getTypeParameters().toArray(TypeParameterElement[]::new);
        String[] typeParameterNames = Arrays.stream(typeParameters).map(Object::toString).toArray(String[]::new);

        Transform[] transforms = Arrays.stream(template.value()).map(instantiation -> {
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

            String targetClassName = template.append()
                    ? sourceClassName + typeNames
                    : typeNames + sourceClassName;

            List<Replace> customAndTypeReplaces = new ArrayList<>(Arrays.asList(instantiation.replace()));
            customAndTypeReplaces.add(new ReplaceImpl(sourceClassName + "\\s*<[\\s\\w\\?,]+>\\s*\\{", targetClassName + " {", true));
            for (int i = 0; i < typeParameterNames.length; i++) {
                String from = "\\b" + typeParameterNames[i] + "\\b";
                String to = concreteTypeNames[i];
                customAndTypeReplaces.add(new ReplaceImpl(from, to, true));
            }

            return new TransformImpl(targetClassName, customAndTypeReplaces.toArray(Replace[]::new));
        }).toArray(Transform[]::new);

    process(
            sourceClass,
            template.relativeSourceDir(),
            Template.class,
            transforms,
            messager);
    }

    private void process(
            TypeElement sourceClass,
            String relativeSourceDir,
            Class<? extends Annotation> annotationType,
            Transform[] transforms,
            Messager messager) {
        // read source file
        Path sourceDir = findSourceDirectory(relativeSourceDir, messager);
        String sourceCode = readSourceCode(sourceDir, sourceClass, messager);
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();

        // make sure that transforms arrays are consistent
        if (transforms.length == 0) {
            throw new CodeGeneratorException("No transforms supplied");
        }

        // generate target files
        String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
        for (Transform transform : transforms) {
            String fullyQualifiedTargetClassName = pkg + "." + transform.target();
            if (fullyQualifiedTargetClassName.equals(fullyQualifiedSourceClassName)) {
                throw new CodeGeneratorException(
                        "Target class name must be different from source class name, but was " + fullyQualifiedTargetClassName);
            }
            messager.printMessage(NOTE, "Creating " + fullyQualifiedTargetClassName + " from " + fullyQualifiedSourceClassName);
            String targetCode = generateTarget(
                    fullyQualifiedSourceClassName,
                    fullyQualifiedTargetClassName,
                    annotationType,
                    sourceCode,
                    transform.replace());

            writeFile(targetCode, fullyQualifiedTargetClassName, processingEnv);
        }
    }

    private static String generateTarget(
            String fullyQualifiedSourceClassName,
            String fullyQualifiedTargetClassName,
            Class<? extends Annotation> annotationType,
            String sourceCode,
            Replace[] replacements) {
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String targetClassName = FQ_TO_CLASS.apply(fullyQualifiedTargetClassName);

        String targetCode = sourceCode;

        targetCode = replace(replacements, targetCode, fullyQualifiedSourceClassName);

        targetCode = removeImport(targetCode, CodeTransformer.class.getName());
        targetCode = removeImport(targetCode, Transform.class.getName());
        targetCode = removeImport(targetCode, Template.class.getName());
        targetCode = removeImport(targetCode, Instantiate.class.getName());
        targetCode = removeImport(targetCode, Replace.class.getName());

        targetCode = removeAnnotation(targetCode, annotationType, fullyQualifiedSourceClassName);

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
