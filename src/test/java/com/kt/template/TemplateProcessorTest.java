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
                public class Klass<T extends Thing<U<V<W>>>> {
                    private List<T> list;
                    private Test test;
                }
                """;

        String expected = """
                package com.kt;
                            
                import some.pkg.Foo;
                            
                public class KlassFoo {
                    private List<Foo> list;
                    private Test test;
                }
                """;

        assertEquals(expected, toString(generateSource("com.kt.Klass", toList(in), List.of("T"), List.of("Foo"))));
    }

    @Test
    public void bug1() {
        String in = """
                package com.kt;
                            
                import com.kt.template.Template;
                import some.pkg.Foo;
                import some.pkg.Bar;
                            
                @Template(
                        types1 = {Double.class, String.class},
                        types2 = {Foo.class, Bar.class}
                )
                public class Klass<T1 extends Number, T2> {
                    private T1 t1;
                    private T2 t2;
                            
                    // ...
                }
                """;

        String expected1 = """
                package com.kt;
                            
                import some.pkg.Foo;
                import some.pkg.Bar;
                            
                public class KlassDoubleFoo {
                    private Double t1;
                    private Foo t2;
                    
                    // ...
                }
                """;

        assertEquals(expected1, toString(generateSource("com.kt.Klass", toList(in), List.of("T1", "T2"), List.of("Double", "Foo"))));

        String expected2 = """
                package com.kt;
                            
                import some.pkg.Foo;
                import some.pkg.Bar;
                            
                public class KlassStringBar {
                    private String t1;
                    private Bar t2;
                    
                    // ...
                }
                """;

        assertEquals(expected2, toString(generateSource("com.kt.Klass", toList(in), List.of("T1", "T2"), List.of("String", "Bar"))));
    }

    private static List<String> toList(String s) {
        return Arrays.asList(s.split("\n"));
    }

    private static String toString(List<String> list) {
        return String.join("\n", list) + "\n";
    }
}



