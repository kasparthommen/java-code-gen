package com.kt.codegen;


import org.junit.jupiter.api.Test;

import static com.kt.codegen.CodeGeneratorTestHelper.checkGeneration;


public class CodeGeneratorProcessorDeriveTest {
    @Test
    public void classRename() throws Exception {
        checkGeneration(
                new CodeGeneratorProcessor(),

                "x.y.Before",

                """
                package x.y;
                
                import com.kt.codegen.Derivatives;
                import com.kt.codegen.Derive;
                import com.kt.codegen.Replace;

                @Derive(name = "After", replace = {})
                public  class Before<T, U> {  // Beforex 1Before Before
                  int q;
                  
                  public Before(int q) {
                    this.q = q;
                  }
                }
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U> {  // Beforex 1Before After
                  int q;
                  
                  public After(int q) {
                    this.q = q;
                  }
                }
                """);
}

    @Test
    public void klassRenameAndReplacements() throws Exception {
        for (String cheekyString : new String[] { "(", ")", "[", "]", "{", "}", "@", "\\\"" }) {
            checkGeneration(
                    new CodeGeneratorProcessor(),

                    "z.x.y.Before",

                    """
                    package x.y;
                    
                    import com.kt.codegen.Derive;
                    import com.kt.codegen.SourceDirectory;
                    import com.kt.codegen.Replace;
    
                         @ Derive ( name    ="After"
                         , replace=     {
                        @Replace(from="q", to="p"),
                        @Replace(from = "int",
                          to = "long"),  @Replace(from = "xx", to="trying to make the parser stumble: $$$$")
                    } )
                    
                      @ SourceDirectory("../../src/main/java/z/")
                    
                    @Derive(name="After2")
                     
                    public  class Before<T, U> {
                      int int1;  // xx
                      
                      public Before(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString),

                    "x.y.After",

                    """
                    // generated from x.y.Before
                    package x.y;
                    
                    public  class After<T, U> {
                      long long1;  // trying to make the parser stumble: $$$$
                      
                      public After(long p) {
                        this.long1 = p;
                      }
                    }
                    """.replace("$$$$", cheekyString.replace("\\\"", "\"")));

            checkGeneration(
                    new CodeGeneratorProcessor(),

                    "x.y.Before",

                    """
                    package x.y;
                    
                    import com.kt.codegen.Derive;
                    import com.kt.codegen.SourceDirectory;
                    import com.kt.codegen.Replace;
    
                         @ Derive ( name    ="After"
                         , replace=     {
                        @Replace(from="q", to="p"),
                        @Replace(from = "int",
                          to = "long"),  @Replace(from = "xx", to="trying to make the parser stumble: $$$$")
                    } )
                    
                      @ SourceDirectory("../../src/main/java/z/")
                    
                    @Derive(name="After2")
                     
                    public  class Before<T, U> {
                      int int1;  // xx
                      
                      public Before(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString),

                    "x.y.After2",

                    """
                    // generated from x.y.Before
                    package x.y;
                    
                    public  class After2<T, U> {
                      int int1;  // xx
                      
                      public After2(int q) {
                        this.int1 = q;
                      }
                    }
                    """.replace("$$$$", cheekyString.replace("\\\"", "\"")));
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
                    com.kt.codegen.Derive
                     ;
                import com.kt.codegen.Derive;

                @Derive(name = "After", replace = {})
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

                
                import com.kt.codegen.Derive;

                @Derive(name = "After", replace = {})

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

                import com.kt.codegen.Derive;
                

                @Derive(name = "After", replace = {})

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


                import com.kt.codegen.Derive;
                

                @Derive(name = "After", replace = {})
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
                import com.kt.codegen.Derive;

                @Derive(name = "After", replace = {})
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
                
                import com.kt.codegen.Derive;
                import java.util.Date;

                @Derive(name = "After", replace = {})
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
                import com.kt.codegen.Derive;
                import java.util.Date;

                @Derive(name = "After", replace = {})
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
