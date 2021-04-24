package com.kt.template;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.kt.template.TemplateProcessor.generateSource;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TemplateProcessorTest {
    @Test
    public void oneTypeArg() {
        String in = """
                package com.kt;
                            
                import com.kt.template.Template;
                import some.pkg.Foo;
                
                @Template(types1={Foo.class})
                public class Klass<T
                                     extends
                                      Thing<U<V<W>
                                      > >>   {
                    private List<T> list;
                    private Test test;
                    
                    public Klass(int arg) {
                        this.arg = arg;
                    }
                }
                """;

        String expected = """
                package com.kt;
                            
                import some.pkg.Foo;
                
                // generated from com.kt.Klass
                public class KlassFoo   {
                    private List<Foo> list;
                    private Test test;
                    
                    public KlassFoo(int arg) {
                        this.arg = arg;
                    }
                }
                """;

        assertEquals(expected, toString(generateSource(
                "com.kt.Klass",
                "com.kt.KlassFoo",
                toList(in),
                new String[] { "T" },
                new String[] { "some.pkg.Foo" },
                new String[] {})));
    }

    @Test
    public void interfaceOneTypeArg() {
        String in = """
                @Template(
                        types1 = { int.class, long.class }
                )
                public interface PrimitiveSequence<T> extends ISequence {
                    default T get(int index) {
                        checkIndex(index);
                        return getUnsafe(index);
                    }
                
                    T get(int index);
                }
                """;

        String expected = """
                // generated from PrimitiveSequence
                public interface PrimitiveSequenceInt extends ISequence {
                    default int get(int index) {
                        checkIndex(index);
                        return getUnsafe(index);
                    }
                
                    int get(int index);
                }
                """;

        assertEquals(expected, toString(generateSource(
                "PrimitiveSequence",
                "PrimitiveSequenceInt",
                toList(in),
                new String[] { "T" },
                new String[] { "int" },
                new String[] {})));
    }

    @Test
    public void twoTypeArgsWithReplacement() {
        String[] replacements = {
                "double", "(T1[]) new Object", "new  double ",
                "some.pkg.Bar", "new T2()", "Bar.create()",
        };

        String in = """
                package com.kt;
                            
                import com.kt.template.Template;
                import some.pkg.Foo;
                import some.pkg.Bar;
                
                @Template(
                        types1 = {Double.class, String.class},
                        types2 = {Foo.class, Bar.class}
                )
                public class Klass<T1 extends Whatever, T2> {
                    private T1 t1;
                    private T2 t2;
                            
                    void x() {
                        T1[] array = (T1[]) new Object[42];
                        T2 t = new T2();
                    }
                }
                """;

        String expected1 = """
                package com.kt;
                            
                import some.pkg.Foo;
                import some.pkg.Bar;
                
                // generated from com.kt.Klass
                public class KlassDoubleFoo {
                    private double t1;
                    private Foo t2;
                    
                    void x() {
                        double[] array = new  double [42];
                        Foo t = new Foo();
                    }
                }
                """;

        assertEquals(expected1, toString(generateSource(
                "com.kt.Klass",
                "com.kt.KlassDoubleFoo",
                toList(in),
                new String[] { "T1", "T2" },
                new String[] { "double", "some.pkg.Foo" },
                replacements)));

        String expected2 = """
                package com.kt;
                            
                import some.pkg.Foo;
                import some.pkg.Bar;
                
                // generated from com.kt.Klass
                public class KlassStringBar {
                    private String t1;
                    private Bar t2;
                    
                    void x() {
                        String[] array = (String[]) new Object[42];
                        Bar t = Bar.create();
                    }
                }
                """;

        assertEquals(expected2, toString(generateSource(
                "com.kt.Klass",
                "com.kt.KlassStringBar",
                toList(in),
                new String[] { "T1", "T2" },
                new String[] { "java.lang.String", "some.pkg.Bar" },
                replacements)));
    }

    private static List<String> toList(String s) {
        return Arrays.asList(s.split("\n"));
    }

    private static String toString(List<String> list) {
        return String.join("\n", list) + "\n";
    }
}



