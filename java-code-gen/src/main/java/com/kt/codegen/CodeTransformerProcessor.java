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
import java.nio.file.Path;
import java.util.Set;

import static com.kt.codegen.CodeGenerationHelper.FQ_TO_CLASS;
import static com.kt.codegen.CodeGenerationHelper.FQ_TO_PACKAGE;
import static com.kt.codegen.CodeGenerationHelper.findSourceDirectory;
import static com.kt.codegen.CodeGenerationHelper.readSourceCode;
import static com.kt.codegen.CodeGenerationHelper.removeAnnotation;
import static com.kt.codegen.CodeGenerationHelper.removeImport;
import static com.kt.codegen.CodeGenerationHelper.replace;
import static com.kt.codegen.CodeGenerationHelper.writeFile;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


@SupportedAnnotationTypes("com.kt.codegen.CodeTransformer")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class CodeTransformerProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        for (Element element : roundEnv.getElementsAnnotatedWith(CodeTransformer.class)) {
            try {
                process(element, messager);
            } catch (CodeGenerationException ex) {
                messager.printMessage(ERROR, ex.getMessage());
            }
        }
        return true;
    }

    private void process(Element element, Messager messager) throws CodeGenerationException {
        TypeElement sourceClass = (TypeElement) element;
        messager.printMessage(NOTE, "Creating code transform for " + sourceClass.getQualifiedName());
        CodeTransformer transformer = sourceClass.getAnnotation(CodeTransformer.class);

        // read source file
        Path sourceDir = findSourceDirectory(transformer.relativeSourceDir(), messager);
        String sourceCode = readSourceCode(sourceDir, sourceClass, messager);
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();

        // make sure that transforms arrays are consistent
        Transform[] transforms = transformer.transforms();
        if (transforms.length == 0) {
            throw new CodeGenerationException("No transforms supplied");
        }

        // generate target files
        String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
        for (Transform transform : transforms) {
            String fullyQualifiedTargetClassName = pkg + "." + transform.targetName();
            if (fullyQualifiedTargetClassName.equals(fullyQualifiedSourceClassName)) {
                throw new CodeGenerationException(
                        "Target class name must be different from source class name, but was " + fullyQualifiedTargetClassName);
            }
            messager.printMessage(NOTE, "Creating " + fullyQualifiedTargetClassName + " from " + fullyQualifiedSourceClassName);
            String targetCode = generateTarget(
                    fullyQualifiedSourceClassName,
                    fullyQualifiedTargetClassName,
                    sourceCode,
                    transform.replacements());

            writeFile(targetCode, fullyQualifiedTargetClassName, processingEnv);
        }
    }

    private static String generateTarget(
            String fullyQualifiedSourceClassName,
            String fullyQualifiedTargetClassName,
            String sourceCode,
            Replace[] replacements) throws CodeGenerationException {
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String targetClassName = FQ_TO_CLASS.apply(fullyQualifiedTargetClassName);

        String targetCode = sourceCode;

        targetCode = replace(replacements, targetCode, fullyQualifiedSourceClassName);

        targetCode = removeImport(targetCode, CodeTransformer.class.getName());
        targetCode = removeImport(targetCode, Transform.class.getName());
        targetCode = removeImport(targetCode, Replace.class.getName());

        targetCode = removeAnnotation(targetCode, CodeTransformer.class, fullyQualifiedSourceClassName);

        targetCode = targetCode.replaceAll("\\b" + sourceClassName + "\\b", targetClassName);
        targetCode = "// generated from " + fullyQualifiedSourceClassName + "\n" + targetCode;

        return targetCode;
    }
}
