package com.kt.codegen;


import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import javax.annotation.processing.AbstractProcessor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;


class CodeGeneratorTestHelper {
    static void checkGeneration(
            AbstractProcessor annotationProcessor,
            String sourceClassName,
            String source,
            String expectedTargetClassName,
            String expectedTarget) throws Exception {
        saveSourceFileAndScheduleCleanup(sourceClassName, source);
        Compilation compilation = javac()
                .withProcessors(annotationProcessor)
                .compile(JavaFileObjects.forSourceString(sourceClassName, source));
        assertThat(compilation).succeeded();

        // if this fails with the following error, then you'll have to add the "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
        // option to the JVM running this test:
        //
        // java.lang.IllegalAccessError: class com.google.testing.compile.Parser (in unnamed module @0xxxxxxxx) cannot access class
        // com.sun.tools.javac.api.JavacTool (in module jdk.compiler) because module jdk.compiler does not export com.sun.tools.javac.api
        // to unnamed module @0x7823a2f9
        assertThat(compilation)
                .generatedSourceFile(expectedTargetClassName)
                .contentsAsString(StandardCharsets.UTF_8).isEqualTo(expectedTarget);
    }

    private static void saveSourceFileAndScheduleCleanup(String fullyQualifiedClassName, String source) throws Exception {
        // This is a bit hacky as we literally store the source file in src/main/java. This is necessary because
        // that's where the code generator looks for the source file. We make sure though that we clean things
        // up upon JVM shutdown. Note that calling deleteOnExit() on the package root directory doesn't seem to
        // be working, so we do recursive cleanup manually in a shutdown hook.
        Path classFileDir = Path.of(CodeGeneratorTestHelper.class.getClassLoader().getResource(".").toURI());
        Path javaRoot = classFileDir.resolve("../../src/main/java/").normalize();
        Path sourceFile = javaRoot.resolve(fullyQualifiedClassName.replace(".", File.separator) + ".java");
        final Path rootToDeleteOnExit = javaRoot.resolve(fullyQualifiedClassName.split("\\.")[0]);
        rootToDeleteOnExit.toFile().deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                deleteRecursively(rootToDeleteOnExit);
            }

            private void deleteRecursively(Path path) {
                try {
                    if (Files.isDirectory(path)) {
                        try (Stream<Path> list = Files.list(path)) {
                            list.forEach(this::deleteRecursively);
                        }
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
    }
}
