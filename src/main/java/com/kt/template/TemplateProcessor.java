package com.kt.template;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

@SupportedAnnotationTypes("com.kt.template.Template")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
@AutoService(Processor.class)
public class TemplateProcessor extends AbstractProcessor {
    private static final char[] TOKEN_BOUNDARIES = {
            ' ',
            '\t',
            '\n',
            '(',
            ')',
            '[',
            ']',
            '{',
            '}',
            ',',
            '<',
            '>',
            ',',
    };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Template.class)) {
            TypeElement sourceClass = (TypeElement) element;

            Messager messager = processingEnv.getMessager();
            messager.printMessage(NOTE, "Instantiating class templates for " + sourceClass.getClass());

            Template template = sourceClass.getAnnotation(Template.class);

            // find source file
            Path sourceDir = Path.of("").toAbsolutePath().resolve(template.sourceDir());
            if (!Files.exists(sourceDir)) {
                messager.printMessage(ERROR, "Source path not found: " + sourceDir
                        + ". Possibly a mis-specification of the relative source directory (" + template.sourceDir() + ")?");
            }
            messager.printMessage(NOTE, "sourceDir=" + sourceDir);
            String packagePlusSourceClassName = sourceClass.getQualifiedName().toString();
            String relativePath = packagePlusSourceClassName.replace(".", File.separator) + ".java";
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
                return false;
            }

            // read desired concrete types
            messager.printMessage(NOTE, template.toString());
            List<? extends TypeMirror> types1 = getTypes(template::types1, messager);
            List<? extends TypeMirror> types2 = getTypes(template::types2, messager);

            // make sure the number of declared type parameters matches the given number of type parameter values
            int typesCount = (types1.isEmpty() ? 0 : 1) + (types2.isEmpty() ? 0 : 1);
            List<? extends TypeParameterElement> typeParameters = sourceClass.getTypeParameters();
            if (typeParameters.size() != typesCount) {
                messager.printMessage(ERROR, "Expected " + typeParameters.size() + " type parameters, got " + typesCount);
                return false;
            }

            // generate concrete template instantiation source files
            List<String> typeParameterNames = typeParameters.stream().map(Object::toString).collect(toList());
            Function<String, String> stripPackage = (String s) -> s.substring(s.lastIndexOf('.') + 1);
            for (TypeMirror type1 : types1) {
                for (TypeMirror type2 : types2) {
                    List<String> concreteTypeNames = Stream.of(type1, type2).map(TypeMirror::toString).map(stripPackage).collect(toList());

                    List<String> concreteSource = generateSource(
                            packagePlusSourceClassName,
                            genericSource,
                            typeParameterNames,
                            concreteTypeNames
                    );

                    String packagePlusConcreteClassName = packagePlusSourceClassName + concreteTypeNames.stream().map(stripPackage).collect(joining(""));
                    writeFile(concreteSource, packagePlusConcreteClassName, messager);
                }
            }
        }

        return true;
    }

    private static List<? extends TypeMirror> getTypes(Supplier<Class<?>[]> s, Messager messager) {
        // uses this trick:
        // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation/52793839#52793839
        try {
            s.get();
        } catch (MirroredTypesException mtex) {
            return mtex.getTypeMirrors();
        }
        messager.printMessage(ERROR, "Illegal state");
        return null;
    }

    public static List<String> generateSource(
            String packagePlusSourceClassName,
            List<String> genericSource,
            List<String> typeParameterNames,
            List<String> concreteTypeNames) {
        Function<String, String> stripPackage = (String s) -> s.substring(s.lastIndexOf('.') + 1);

        String sourceClassName = stripPackage.apply(packagePlusSourceClassName);
        String packagePlusGeneratedClassName = packagePlusSourceClassName + concreteTypeNames.stream().map(stripPackage).collect(joining(""));

        boolean removeTemplateAnnotation = true;
        String templateString = "@Template";
        BracketSkipper templateSkipper = null;

        boolean replaceClassDeclaration = true;
        String oldClassDeclaration = "class " + sourceClassName;
        BracketSkipper typeArgsSkipper = null;

        List<String> concreteSource = new ArrayList<>();
        for (String line : genericSource) {
            // remote Template import
            if (line.startsWith("import " + Template.class.getName())) {
                continue;
            }

            // remote @Template(...) annotation
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
                String beforeLine = "";
                if (typeArgsSkipper == null) {
                    int classDeclarationIndex = line.indexOf(oldClassDeclaration);
                    if (classDeclarationIndex != -1) {
                        typeArgsSkipper = new BracketSkipper('<', '>');
                        beforeLine = line.substring(0, classDeclarationIndex);
                        line = line.substring(classDeclarationIndex + oldClassDeclaration.length());
                    }
                }

                if (typeArgsSkipper != null) {
                    String processed = typeArgsSkipper.process(line);
                    if (processed == null) {
                        continue;
                    } else {
                        String newClassDeclaration = "class " + stripPackage.apply(packagePlusGeneratedClassName);
                        line = newClassDeclaration + processed;
                        replaceClassDeclaration = false;
                    }
                }

                line = beforeLine + line;
            }

            // replace generic types by concrete ones
            for (int i = 0; i < typeParameterNames.size(); i++) {
                if (line.contains(typeParameterNames.get(i))) {
                    line = replaceToken(line, typeParameterNames.get(i), concreteTypeNames.get(i));
                }
            }

            concreteSource.add(line);
        }

        return concreteSource;
    }

    private void writeFile(List<String> source, String packagePlusConcreteClassName, Messager messager) {
        try {
            JavaFileObject targetFile = processingEnv.getFiler().createSourceFile(packagePlusConcreteClassName);
            try (PrintWriter targetWriter = new PrintWriter(targetFile.openWriter())) {
                for (String line : source) {
                    targetWriter.println(line);
                }
            }
        } catch (IOException e) {
            messager.printMessage(ERROR, "Could not generate file " + packagePlusConcreteClassName);
        }
    }

    private static String replaceToken(String s, String from, String to) {
        for (char before : TOKEN_BOUNDARIES) {
            for (char after : TOKEN_BOUNDARIES) {
                s = s.replace(before + from + after, before + to + after);
            }
        }
        return s;
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
        private int bracketCounter = 0;

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
