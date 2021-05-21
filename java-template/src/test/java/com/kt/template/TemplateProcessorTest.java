package com.kt.template;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.kt.template.CodeGenerationTestHelper.checkTransform;


public class TemplateProcessorTest {
    @Test
    public void oneTypeArg() throws Exception {
        String source = """
                package x.y;
                            
                import com.kt.template.Template;
                import com.kt.template.Instantiation;
                import java.util.Date;
                import java.util.List;
                
                @
                 Template  ( instantiations = @Instantiation(types = {Date.class}))
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
                
                public class KlassDate   {
                    private List<Date> list;
                    
                    public KlassDate(Date arg) {
                    }
                }
                """;

        checkTransform(new TemplateProcessor(), "x.y.Klass", source, "x.y.KlassDate", expectedTarget);
    }

    @Test
    public void interfaceOneTypeArg() throws Exception {
        String source = """
                package x.y;
                import com.kt.template.Template;
                import com.kt.template.Instantiation;
                
                @Template(instantiations = {
                        @Instantiation(types = { int.class })
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

        checkTransform(new TemplateProcessor(), "x.y.PrimitiveSequence", source, "x.y.PrimitiveSequenceInt", expectedTarget);
    }

    @Test
    public void twoTypeArgsWithReplacement() throws Exception {
        String[] replacements = {
                "double", "(T1[]) new Object", "new  double ",
                "some.pkg.Bar", "new T2()", "Bar.create()",
        };

        String source = """
                package x.y;
                            
                import com.kt.template.Template;
                import com.kt.template.Instantiation;
                import com.kt.template.Replace;
                import com.kt.template.ReplaceType;
                import com.kt.template.TypeNamePosition;
                import java.util.Date;
                
                @Template(
                    typeNamePosition = TypeNamePosition.PREPEND,
                    instantiations = {
                        @Instantiation(
                            types = { double.class, Date.class },
                            replacements = {
                                @Replace(from = "(T1[]) new Object", to = "new  double "),
                                @Replace(from = "= null", to = "= new Date(0)")
                        }),
                        @Instantiation(
                            types = { String.class, Float.class },
                            replacements = {
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

        checkTransform(new TemplateProcessor(), "x.y.Klass", source, "x.y.DoubleDateKlass", expectedTarget1);

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

        checkTransform(new TemplateProcessor(), "x.y.Klass", source, "x.y.StringFloatKlass", expectedTarget2);
    }

    private static List<String> toList(String s) {
        return Arrays.asList(s.split("\n"));
    }

    private static String toString(List<String> list) {
        return String.join("\n", list) + "\n";
    }
}



