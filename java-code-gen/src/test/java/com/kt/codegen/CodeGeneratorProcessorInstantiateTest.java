package com.kt.codegen;


import org.junit.jupiter.api.Test;

import static com.kt.codegen.CodeGenerationTestHelper.checkGeneration;


public class CodeGeneratorProcessorInstantiateTest {
    @Test
    public void oneTypeArg() throws Exception {
        String source = """
                package x.y;
                            
                import com.kt.codegen.Instantiations;
                import com.kt.codegen.Instantiate;
                import com.kt.codegen.SourceDirectory;
                import java.util.Date;
                import java.util.List;
                
                
                 @ SourceDirectory("../../src/main/java")

                @
                  Instantiate(Date.class)
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

        checkGeneration(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.KlassDate", expectedTarget);
    }

    @Test
    public void interfaceOneTypeArg() throws Exception {
        String source = """
                package x.y;
                import com.kt.codegen.Instantiate;
                
                @Instantiate({ int.class })
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

        checkGeneration(new CodeGeneratorProcessor(), "x.y.PrimitiveSequence", source, "x.y.PrimitiveSequenceInt", expectedTarget);
    }

    @Test
    public void twoTypeArgsWithReplacement() throws Exception {
        String source = """
                package x.y;
                            
                import com.kt.codegen.Instantiate;
                import com.kt.codegen.Replace;
                import java.util.Date;
                
                @Instantiate(
                    value = { double.class, Date.class },
                    replace = {
                        @Replace(from = "(T1[]) new Object", to = "new  double "),
                        @Replace(from = "= null", to = "= new Date(0)")
                    }, append = false
                )
                @Instantiate(
                    value = { String.class, Float.class },
                    replace = {
                        @Replace(from = "(T1[]) new Object", to = "new String"),
                        @Replace(from = "= null", to = "= Float.NaN")
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

        checkGeneration(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.DoubleDateKlass", expectedTarget1);

        String expectedTarget2 = """
                // generated from x.y.Klass
                package x.y;
                            
                import java.util.Date;
                
                public class KlassStringFloat {
                    private String t1;
                    private Float t2;
                    
                    void x() {
                        String[] array = new String[42];
                        Float t = Float.NaN;
                    }
                }
                """;

        checkGeneration(new CodeGeneratorProcessor(), "x.y.Klass", source, "x.y.KlassStringFloat", expectedTarget2);
    }
}
