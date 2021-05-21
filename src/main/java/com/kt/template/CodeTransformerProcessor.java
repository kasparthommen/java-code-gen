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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.kt.template.CodeGenerationHelper.FQ_TO_CLASS;
import static com.kt.template.CodeGenerationHelper.FQ_TO_PACKAGE;
import static com.kt.template.CodeGenerationHelper.findSourceDirectory;
import static com.kt.template.CodeGenerationHelper.readSourceCode;
import static com.kt.template.CodeGenerationHelper.removeAnnotation;
import static com.kt.template.CodeGenerationHelper.removeImport;
import static com.kt.template.CodeGenerationHelper.replace;
import static com.kt.template.CodeGenerationHelper.writeFile;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


@SupportedAnnotationTypes("com.kt.template.CodeTransformer")
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
        messager.printMessage(NOTE, "Creating class replacements for " + sourceClass.getQualifiedName());
        CodeTransformer transformer = sourceClass.getAnnotation(CodeTransformer.class);

        // read source file
        Path sourceDir = findSourceDirectory(transformer.relativeSourceDir(), messager);
        String sourceCode = readSourceCode(sourceDir, sourceClass, messager);
        String fullyQualifiedSourceClassName = sourceClass.getQualifiedName().toString();

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
            throw new CodeGenerationException("No replacements supplied");
        }

        // generate target files
        String pkg = FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName);
        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        for (int i = 0; i < transforms.size(); i++) {
            Transform transform = transforms.get(i);

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
                    transform.replacements()
            );

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

        targetCode = removeImport(targetCode, CodeTransformer.class.getName());
        targetCode = removeImport(targetCode, Transform.class.getName());
        targetCode = removeImport(targetCode, Replace.class.getName());

        targetCode = removeAnnotation(targetCode, CodeTransformer.class, fullyQualifiedSourceClassName);

        targetCode = replace(replacements, targetCode, fullyQualifiedSourceClassName);

        targetCode = targetCode.replaceAll("\\b" + sourceClassName + "\\b", targetClassName);
        targetCode = "// generated from " + fullyQualifiedSourceClassName + "\n" + targetCode;

        return targetCode;
    }
}
