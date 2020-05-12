package com.kt.template;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.kt.template.TemplateProcessor.generateSource;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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

        assertThat(toString(generateSource("com.kt.Klass", toList(in), List.of("T"), List.of("Foo"))), equalTo(expected));
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

        assertThat(toString(generateSource("com.kt.Klass", toList(in), List.of("T1", "T2"), List.of("Double", "Foo"))), equalTo(expected1));

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

        assertThat(toString(generateSource("com.kt.Klass", toList(in), List.of("T1", "T2"), List.of("String", "Bar"))), equalTo(expected2));
    }

    private static List<String> toList(String s) {
        return Arrays.asList(s.split("\n"));
    }

    private static String toString(List<String> list) {
        return String.join("\n", list) + "\n";
    }
}



