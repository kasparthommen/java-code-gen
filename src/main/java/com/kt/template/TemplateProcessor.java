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
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


@SupportedAnnotationTypes("com.kt.template.Template")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class TemplateProcessor extends AbstractProcessor {
    private static final Function<String, String> FQ_TO_CLASS = s -> s.substring(s.lastIndexOf('.') + 1);
    private static final Function<String, String> FQ_TO_PACKAGE = s -> s.substring(0, s.lastIndexOf('.'));
    private static final Function<String, String> FIRST_UPPER = s -> s.substring(0, 1).toUpperCase() + s.substring(1);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();

        for (Element element : roundEnv.getElementsAnnotatedWith(Template.class)) {
            TypeElement sourceClass = (TypeElement) element;
            messager.printMessage(NOTE, "Instantiating class templates for " + sourceClass.getQualifiedName());
            Template template = sourceClass.getAnnotation(Template.class);

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
            List<String> genericSource = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    genericSource.add(line);
                }
            } catch (IOException ex) {
                messager.printMessage(ERROR, toString(ex));
                return true;
            }

            // read desired concrete types
            TypeMirror[] types1 = getTypes(template::types1, messager);
            TypeMirror[] types2 = getTypes(template::types2, messager);
            TypeMirror[] types3 = getTypes(template::types3, messager);

            // make sure the number of declared type parameters matches the given number of type parameter values
            int instantiationCount = types1.length;
            if (instantiationCount == 0) {
                messager.printMessage(ERROR, "Must provide at least one concrete type in types1");
                return true;
            }
            int typeArrayCount = 1  // from types1
                    + (types2 != null ? 1 : 0)
                    + (types3 != null ? 1 : 0);
            List<? extends TypeParameterElement> typeParameters = sourceClass.getTypeParameters();
            if (typeParameters.size() != typeArrayCount) {
                messager.printMessage(ERROR, "Expected " + typeParameters.size() + " type parameters, got " + typeArrayCount);
                return true;
            }

            // make sure all type array have the same length
            if (types2 != null && types2.length != instantiationCount) {
                messager.printMessage(ERROR, "Expected " + instantiationCount + " type parameters in types2, got " + types2.length);
                return true;
            }
            if (types3 != null && types3.length != instantiationCount) {
                messager.printMessage(ERROR, "Expected " + instantiationCount + " type parameters in types3, got " + types3.length);
                return true;
            }

            // check optional replacements
            String[] replacements = template.replacements();
            if (replacements.length > 0) {
                if (replacements.length % 3 != 0) {
                    messager.printMessage(
                            ERROR,
                            "Expected replacements array with a series of triplets of the form [className, from, to] but was "
                                    + Arrays.toString(replacements));
                    return true;
                }
            }

            // generate concrete template instantiation source files
            String[] typeParameterNames = typeParameters.stream().map(Object::toString).toArray(String[]::new);
            for (int i = 0; i < instantiationCount; i++) {
                String[] fullyQualifiedConcreteTypeNames = Stream.of(
                        types1[i].toString(),
                        types2 != null ? types2[i].toString() : null,
                        types3 != null ? types3[i].toString() : null
                ).filter(Objects::nonNull).toArray(String[]::new);

                messager.printMessage(NOTE, "Instantiating " + sourceClass.getSimpleName() + " for " + Arrays.toString(fullyQualifiedConcreteTypeNames));
                String typeNames = Arrays.stream(fullyQualifiedConcreteTypeNames)
                                         .map(FQ_TO_CLASS)
                                         .map(FIRST_UPPER)
                                         .collect(joining(""));
                String fullyQualifiedConcreteClassName = template.appendTypeNames()
                        ? fullyQualifiedSourceClassName + typeNames
                        : FQ_TO_PACKAGE.apply(fullyQualifiedSourceClassName) + "." + typeNames + FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
                if (template.classNameReplacement().length != 0) {
                    if (template.classNameReplacement().length != 2) {
                        messager.printMessage(
                                ERROR,
                                "Expected two entries for class name replacement array, but got " + Arrays.toString(template.classNameReplacement()));
                        return true;
                    }
                    String from = template.classNameReplacement()[0];
                    String to = template.classNameReplacement()[1];
                    fullyQualifiedConcreteClassName = fullyQualifiedConcreteClassName.replace(from, to);
                }

                List<String> concreteSource = generateSource(
                        fullyQualifiedSourceClassName,
                        fullyQualifiedConcreteClassName,
                        genericSource,
                        typeParameterNames,
                        fullyQualifiedConcreteTypeNames,
                        replacements);

                writeFile(concreteSource, fullyQualifiedConcreteClassName, messager);
            }
        }

        return true;
    }

    private static  TypeMirror[] getTypes(Supplier<Class<?>[]> s, Messager messager) {
        // uses this trick:
        // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation/52793839#52793839
        try {
            s.get();
        } catch (MirroredTypesException mtex) {
            List<? extends TypeMirror> typeMirrors = mtex.getTypeMirrors();
            return typeMirrors.isEmpty() ? null : typeMirrors.toArray(TypeMirror[]::new);
        }
        messager.printMessage(ERROR, "Illegal state");
        throw new IllegalStateException("Cannot get to here");
    }

    public static List<String> generateSource(
            String fullyQualifiedSourceClassName,
            String fullyQualifiedConcreteClassName,
            List<String> genericSource,
            String[] typeParameterNames,
            String[] fullyQualifiedConcreteTypeNames,
            String[] replacements) {
        assert typeParameterNames.length == fullyQualifiedConcreteTypeNames.length;
        assert replacements.length % 3 == 0;

        String sourceClassName = FQ_TO_CLASS.apply(fullyQualifiedSourceClassName);
        String concreteClassName = FQ_TO_CLASS.apply(fullyQualifiedConcreteClassName);
        String sourceClassDeclaration = "class " + sourceClassName;
        String sourceInterfaceDeclaration = "interface " + sourceClassName;

        boolean removeTemplateAnnotation = true;
        String templateString = "@Template";
        BracketSkipper templateSkipper = null;

        boolean replaceClassDeclaration = true;
        BracketSkipper typeArgsSkipper = null;

        String tempLine = null;

        List<String> concreteSource = new ArrayList<>();
        for (String line : genericSource) {
            // remove Template import
            if (line.startsWith("import " + Template.class.getName())) {
                continue;
            }

            // remove @Template(...) annotation
            if (removeTemplateAnnotation) {
                if (templateSkipper == null) {
                    int templateIndex = line.indexOf(templateString);
                    if (templateIndex != -1) {
                        templateSkipper = new BracketSkipper('(', ')');
                        line = line.substring(templateIndex + templateString.length());
                    }
                }

                if (templateSkipper != null) {
                    String processed = templateSkipper.process(line);
                    if (processed == null) {
                        continue;
                    } else {
                        line = processed;
                        removeTemplateAnnotation = false;
                        if ("".equals(line)) {
                            continue;
                        }
                    }
                }
            }

            // replace class declaration
            if (replaceClassDeclaration) {
                if (typeArgsSkipper == null) {
                    int classDeclarationIndex = line.indexOf(sourceClassDeclaration);
                    int interfaceDeclarationIndex = line.indexOf(sourceInterfaceDeclaration);
                    String declaration = null;
                    int declarationIndex = -1;
                    if (classDeclarationIndex != -1) {
                        declaration = sourceClassDeclaration;
                        declarationIndex = classDeclarationIndex;
                    } else if (interfaceDeclarationIndex != -1) {
                        declaration = sourceInterfaceDeclaration;
                        declarationIndex = interfaceDeclarationIndex;
                    }
                    if (declarationIndex != -1) {
                        typeArgsSkipper = new BracketSkipper('<', '>');
                        tempLine = line.substring(0, declarationIndex + declaration.length());
                    }
                }

                if (typeArgsSkipper != null) {
                    String processed = typeArgsSkipper.process(line);
                    if (processed == null) {
                        continue;
                    } else {
                        line = tempLine + processed;
                        replaceClassDeclaration = false;
                    }
                }
            }

            // process custom replacements
            for (int i = 0; i < replacements.length; i += 3) {
                String typeName = replacements[i];
                String from = replacements[i + 1];
                String to = replacements[i + 2];
                if (List.of(fullyQualifiedConcreteTypeNames).contains(typeName)) {
                    line = line.replace(from, to);
                }
            }

            // replace generic class by concrete class and generic types by concrete ones
            line = line.replace(sourceClassName, concreteClassName);
            for (int i = 0; i < typeParameterNames.length; i++) {
                if (line.contains(typeParameterNames[i])) {
                    line = replaceToken(line, typeParameterNames[i], FQ_TO_CLASS.apply(fullyQualifiedConcreteTypeNames[i]));
                }
            }

            concreteSource.add(line);
        }

        return concreteSource;
    }

    private void writeFile(List<String> source, String fullyQualifiedConcreteClassName, Messager messager) {
        try {
            JavaFileObject targetFile = processingEnv.getFiler().createSourceFile(fullyQualifiedConcreteClassName);
            try (PrintWriter targetWriter = new PrintWriter(targetFile.openWriter())) {
                for (String line : source) {
                    targetWriter.println(line);
                }
            }
        } catch (IOException ex) {
            messager.printMessage(ERROR, "Could not generate file " + fullyQualifiedConcreteClassName + ": " + ex.getMessage());
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


    private static class BracketSkipper {
        private final char start;
        private final char end;
        private int bracketCounter;

        public BracketSkipper(char start, char end) {
            this.start = start;
            this.end = end;
        }

        public String process(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == start) {
                    bracketCounter++;
                } else if (c == end) {
                    bracketCounter--;
                    if (bracketCounter == 0) {
                        return s.substring(i+1);
                    }
                }
            }

            return null;
        }
    }
}
