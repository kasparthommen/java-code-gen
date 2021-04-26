package com.kt.template;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CodeTransformerProcessorTest {
    @Test
    public void klassRename() {
        String source = """
                package x.y;
                
                import com.kt.template.CodeTransformer;

                @CodeTransformer(r1 = { "After" })
                public  class Before<T, U> {  // Beforex 1Before Before
                  int q;
                  
                  public Before(int q) {
                    this.q = q;
                  }
                }
                """;

        String expectedTarget = """
                package x.y;
                
                // generated from x.y.Before
                public  class After<T, U> {  // Beforex 1Before After
                  int q;
                  
                  public After(int q) {
                    this.q = q;
                  }
                }
                """;
        assertEquals(expectedTarget,
                CodeTransformerProcessor.generateTarget(
                        source,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));
    }

    @Test
    public void klassRenameAndReplacements() {
        String source = """
                package x.y;
                
                import com.kt.template.CodeTransformer;

                @CodeTransformer
                
                  (
                  r1 = {
                    "After",
                    "q", "p",
                    "int", "long",
                    "(cheeky parenthesis)", "foo"
                  }
                )
                public  class Before<T, U> {
                  int int1;
                  
                  public Before(int q) {
                    this.int1 = q;
                  }
                }
                """;

        String expectedTarget = """
                package x.y;
                
                // generated from x.y.Before
                public  class After<T, U> {
                  long int1;
                  
                  public After(long p) {
                    this.int1 = p;
                  }
                }
                """;
        assertEquals(expectedTarget,
                CodeTransformerProcessor.generateTarget(
                        source,
                        "x.y.Before", "x.y.After",
                        new String[] { "q", "\\bint\\b" },
                        new String[] { "p", "long" }));
    }

    @Test
    public void importRemoval() {
        assertEquals(
                """
                package x.y;
                
                // generated from x.y.Before
                public  class After<T, U> {}
                """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;
                        
                        import com.kt.template.CodeTransformer;
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                """
                package x.y;

                
                // generated from x.y.Before
                public  class After<T, U> {}
                """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;

                        
                        import com.kt.template.CodeTransformer;
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                """
                package x.y;

                
                // generated from x.y.Before
                public  class After<T, U> {}
                """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;

                        import com.kt.template.CodeTransformer;
                        
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                """
                package x.y;

                
                // generated from x.y.Before
                public  class After<T, U> {}
                """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;


                        import com.kt.template.CodeTransformer;
                        
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                        """
                        package x.y;
                        
                        import a.A;
                        
                        // generated from x.y.Before
                        public  class After<T, U> {}
                        """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;
                        
                        import a.A;
                        import com.kt.template.CodeTransformer;
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                        """
                        package x.y;
                        
                        import b.B;

                        // generated from x.y.Before
                        public  class After<T, U> {}
                        """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;
                        
                        import com.kt.template.CodeTransformer;
                        import b.B;
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));

        assertEquals(
                        """
                        package x.y;
                        
                        import a.A;
                        import b.B;
                        
                        // generated from x.y.Before
                        public  class After<T, U> {}
                        """,
                CodeTransformerProcessor.generateTarget(
                        """
                        package x.y;
                        
                        import a.A;
                        import com.kt.template.CodeTransformer;
                        import b.B;
        
                        @CodeTransformer(r1 = { "After" })
                        public  class Before<T, U> {}
                        """,
                        "x.y.Before", "x.y.After",
                        new String[] {},
                        new String[] {}));
    }
}
