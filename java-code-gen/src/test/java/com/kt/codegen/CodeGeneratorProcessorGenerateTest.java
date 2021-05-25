package com.kt.codegen;


import org.junit.jupiter.api.Test;

import static com.kt.codegen.CodeGenerationTestHelper.checkGeneration;


public class CodeGeneratorProcessorGenerateTest {
    @Test
    public void classRename() throws Exception {
        String source = """
                package x.y;
                
                import com.kt.codegen.Generates;
                import com.kt.codegen.Generate;
                import com.kt.codegen.Replace;

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {  // Beforex 1Before Before
                  int q;
                  
                  public Before(int q) {
                    this.q = q;
                  }
                }
                """;

        String expectedTarget = """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {  // Beforex 1Before After
                  int q;
                  
                  public After(int q) {
                    this.q = q;
                  }
                }
                """;

        checkGeneration(new CodeGeneratorProcessor(), "x.y.Before", source, "x.y.After", expectedTarget);
    }

    @Test
    public void klassRenameAndReplacements() throws Exception {
        for (String cheekyString : new String[] { "(", ")", "[", "]", "{", "}", "@", "\\\"" }) {
            String source = """
                    package x.y;
                    
                    import com.kt.codegen.Generate;
                    import com.kt.codegen.SourceDirectory;
                    import com.kt.codegen.Replace;
    
                         @ Generate ( name    ="After"
                         , replace=     {
                        @Replace(from="q", to="p"),
                        @Replace(from = "int",
                          to = "long"),  @Replace(from = "xx", to="trying to make the parser stumble: $$$$")
                    } )
                    
                      @ SourceDirectory("../../src/main/java")
                    
                    @Generate(name="After2")
                     
                    public  class Before<T, U> {
                      int int1;  // xx
                      
                      public Before(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString);

            String expectedTarget1 = """
                    // generated from x.y.Before
                    package x.y;
                    
                    public  class After<T, U> {
                      long long1;  // trying to make the parser stumble: $$$$
                      
                      public After(long p) {
                        this.long1 = p;
                      }
                    }
                    """.replace("$$$$", cheekyString.replace("\\\"", "\""));

            String expectedTarget2 = """
                    // generated from x.y.Before
                    package x.y;
                    
                    public  class After2<T, U> {
                      int int1;  // xx
                      
                      public After2(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString.replace("\\\"", "\""));

            checkGeneration(new CodeGeneratorProcessor(), "x.y.Before", source, "x.y.After", expectedTarget1);
            checkGeneration(new CodeGeneratorProcessor(), "x.y.Before", source, "x.y.After2", expectedTarget2);
        }
    }

    @Test
    public void importRemoval() throws Exception {
        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                  import
                    com.kt.codegen.Generate
                     ;
                import com.kt.codegen.Generate;

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;

                
                import com.kt.codegen.Generate;

                @Generate(name = "After", replace = {})

                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;

                import com.kt.codegen.Generate;
                

                @Generate(name = "After", replace = {})

                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;

                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;


                import com.kt.codegen.Generate;
                

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import java.lang.Math;
                import com.kt.codegen.Generate;

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                import java.lang.Math;
                
                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import com.kt.codegen.Generate;
                import java.util.Date;

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                import java.util.Date;

                public  class After<T, U> {}
                """);

        checkGeneration(
                new CodeGeneratorProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import java.lang.Math;
                import com.kt.codegen.Generate;
                import java.util.Date;

                @Generate(name = "After", replace = {})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                import java.lang.Math;
                
                import java.util.Date;
                
                public  class After<T, U> {}
                """);
    }
}
