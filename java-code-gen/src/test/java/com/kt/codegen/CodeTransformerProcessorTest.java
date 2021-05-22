package com.kt.codegen;


import org.junit.jupiter.api.Test;

import static com.kt.codegen.CodeGenerationTestHelper.checkTransform;


public class CodeTransformerProcessorTest {
    @Test
    public void classRename() throws Exception {
        String source = """
                package x.y;
                
                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Replace;
                import com.kt.codegen.Transform;

                @CodeTransformer({
                    @Transform(target = "After", replace = {})
                })
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

        checkTransform(new CodeTransformerProcessor(), "x.y.Before", source, "x.y.After", expectedTarget);
    }

    @Test
    public void klassRenameAndReplacements() throws Exception {
        for (String cheekyString : new String[] { "(", ")", "[", "]", "{", "}", "@", "\\\"" }) {
            String source = """
                    package x.y;
                    
                    import com.kt.codegen.CodeTransformer;
                    import com.kt.codegen.Replace;
                    import com.kt.codegen.Transform;
    
                    @CodeTransformer
                    
                      ( {
                         @ Transform ( target    ="After"
                         , replace=     {
                        @Replace(from="q", to="p"),
                        @Replace(from = "int",
                          to = "long"),  @Replace(from = "xx", to="trying to make the parser stumble: $$$$")
                      }) }
                    )
                    public  class Before<T, U> {
                      int int1;  // xx
                      
                      public Before(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString);

            String expectedTarget = """
                    // generated from x.y.Before
                    package x.y;
                    
                    public  class After<T, U> {
                      long long1;  // trying to make the parser stumble: $$$$
                      
                      public After(long p) {
                        this.long1 = p;
                      }
                    }
                    """.replace("$$$$", cheekyString.replace("\\\"", "\""));

            checkTransform(new CodeTransformerProcessor(), "x.y.Before", source, "x.y.After", expectedTarget);
        }
    }

    @Test
    public void importRemoval() throws Exception {
        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                  import    
                    com.kt.codegen.CodeTransformer 
                     ;
                import com.kt.codegen.Transform;

                @CodeTransformer({@Transform(target = "After", replace = {})})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;

                
                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;

                @CodeTransformer({@Transform(target = "After", replace = {})})

                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;

                
                
                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;

                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;
                

                @CodeTransformer({@Transform(target = "After", replace = {})})

                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;

                
                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;


                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;
                

                @CodeTransformer({@Transform(target = "After", replace = {})})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;

                
                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import java.lang.Math;
                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;

                @CodeTransformer({@Transform(target = "After", replace = {})})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                import java.lang.Math;
                
                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;
                import java.util.Date;

                @CodeTransformer({@Transform(target = "After", replace = {})})
                public  class Before<T, U> {}
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                import java.util.Date;

                public  class After<T, U> {}
                """);

        checkTransform(
                new CodeTransformerProcessor(),
                "x.y.Before",

                """
                package x.y;
                
                import java.lang.Math;
                import com.kt.codegen.CodeTransformer;
                import com.kt.codegen.Transform;
                import java.util.Date;

                @CodeTransformer({@Transform(target = "After", replace = {})})
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
