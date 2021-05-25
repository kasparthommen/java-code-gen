package com.kt.codegen;


import org.junit.jupiter.api.Test;

import static com.kt.codegen.CodeGenerationTestHelper.checkTransform;


public class CodeGeneratorProcessorTest {
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

        checkTransform(new CodeGeneratorProcessor(), "x.y.Before", source, "x.y.After", expectedTarget);
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

            checkTransform(new CodeGeneratorProcessor(), "x.y.Before", source, "x.y.After", expectedTarget);
        }
    }

    @Test
    public void importRemoval() throws Exception {
        checkTransform(
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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
                new CodeGeneratorProcessor(),
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

    @Test
    public void templateOneTypeArg() throws Exception {
        String source = """
                package x.y;
                            
                import com.kt.codegen.Template;
                import com.kt.codegen.Instantiate;
                import java.util.Date;
                import java.util.List;
                
                @
                 Template  ( @Instantiate({Date.class}))
                public class Klass<T
                                     extends
                                      Number>   {
                    private List<T> list;
                    
                    public Klass(T arg) {
                    }
                }
                """;

        String expectedTarget = """
                // generated from x.y.Klass
                package x.y;
                            
                import java.util.Date;
                import java.util.List;
                
                public class KlassDate {
                    private List<Date> list;
                    
                    public KlassDate(Date arg) {
                    }
                }
                """;

        checkTransform(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.KlassDate", expectedTarget);
    }

    @Test
    public void templateInterfaceOneTypeArg() throws Exception {
        String source = """
                package x.y;
                import com.kt.codegen.Template;
                import com.kt.codegen.Instantiate;
                
                @Template({
                        @Instantiate({ int.class })
                })
                public interface PrimitiveSequence<T> {
                    default T get(int index) {
                        return getImpl(index);
                    }
                
                    T getImpl(int index);
                }
                """;

        String expectedTarget = """
                // generated from x.y.PrimitiveSequence
                package x.y;

                public interface PrimitiveSequenceInt {
                    default int get(int index) {
                        return getImpl(index);
                    }
                
                    int getImpl(int index);
                }
                """;

        checkTransform(new CodeGeneratorProcessor(), "x.y.PrimitiveSequence", source, "x.y.PrimitiveSequenceInt", expectedTarget);
    }

    @Test
    public void templateTwoTypeArgsWithReplacement() throws Exception {
        String source = """
                package x.y;
                            
                import com.kt.codegen.Template;
                import com.kt.codegen.Instantiate;
                import com.kt.codegen.Replace;
                import java.util.Date;
                
                @Template(
                    append = false,
                    value = {
                        @Instantiate(
                            value = { double.class, Date.class },
                            replace = {
                                @Replace(from = "(T1[]) new Object", to = "new  double "),
                                @Replace(from = "= null", to = "= new Date(0)")
                        }),
                        @Instantiate(
                            value = { String.class, Float.class },
                            replace = {
                                @Replace(from = "(T1[]) new Object", to = "new String"),
                                @Replace(from = "= null", to = "= Float.NaN")
                            }
                        )
                    }
                )
                public class Klass<T1 extends Number, T2> {
                    private T1 t1;
                    private T2 t2;
                            
                    void x() {
                        T1[] array = (T1[]) new Object[42];
                        T2 t = null;
                    }
                }
                """;

        String expectedTarget1 = """
                // generated from x.y.Klass
                package x.y;
                            
                import java.util.Date;
                
                public class DoubleDateKlass {
                    private double t1;
                    private Date t2;
                    
                    void x() {
                        double[] array = new  double [42];
                        Date t = new Date(0);
                    }
                }
                """;

        checkTransform(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.DoubleDateKlass", expectedTarget1);

        String expectedTarget2 = """
                // generated from x.y.Klass
                package x.y;
                            
                import java.util.Date;
                
                public class StringFloatKlass {
                    private String t1;
                    private Float t2;
                    
                    void x() {
                        String[] array = new String[42];
                        Float t = Float.NaN;
                    }
                }
                """;

        checkTransform(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.StringFloatKlass", expectedTarget2);
    }
}
