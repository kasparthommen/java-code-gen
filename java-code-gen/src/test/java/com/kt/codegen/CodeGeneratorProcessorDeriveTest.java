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
                public  class Before<T, U>    implements     Comparable<Before> {  // Beforex 1Before Before
                  int q;
                  
                  public Before(int q) {
                    this.q = q;
                  }
                  
                  public int compareTo(Before other) { return 0; }
                }
                """,

                "x.y.After",

                """
                // generated from x.y.Before
                package x.y;
                
                public  class After<T, U>    implements     Comparable<After> {  // Beforex 1Before After
                  int q;
                  
                  public After(int q) {
                    this.q = q;
                  }
                  
                  public int compareTo(After other) { return 0; }
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
                    
                    @Derive(name="After2", replace = {})
                     
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
                    
                    @Derive(name="After2", replace = {})
                     
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

    @Test
    public void testBug() throws Exception {
        checkGeneration(
                new CodeGeneratorProcessor(),

                "x.y.DoubleDictSequence8",

                """
                package x.y;
                
                import com.kt.codegen.Derive;
                import com.kt.codegen.Replace;

                @Derive(name = "IntDictSequence16", replace = {
                    @Replace(from = "byte", to = "short"),
                    @Replace(from = "Byte", to = "Short"),
                    @Replace(from = "double", to = "int"),
                    @Replace(from = "Double", to = "Int"),
                    @Replace(from = "1 << 8", to = "1 << 16"),
                })
                class DoubleDictSequence8 {
                    private final double[] universe;
                    private final byte[] indices;
                
                    DoubleDictSequence8(double[] universe, byte[] indices) {
                        assert universe.length <= 1 << 8;
                
                        this.universe = universe;
                        this.indices = indices;
                    }
                }
                """,

                "x.y.IntDictSequence16",

                """
                // generated from x.y.DoubleDictSequence8
                package x.y;
                
                class IntDictSequence16 {
                    private final int[] universe;
                    private final short[] indices;
                
                    IntDictSequence16(int[] universe, short[] indices) {
                        assert universe.length <= 1 << 16;
                
                        this.universe = universe;
                        this.indices = indices;
                    }
                }
                """
        );
    }
}
