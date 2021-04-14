# Java Template
## Introduction
An approach to emulate C++ templates. Works by generating class instantiations with concrete types using a simple
annotation. Helps to avoid virtual method calls.

**This is experimental and unfinished code. Please use at your own risk.**

## Example
Say you have the following generic class with two type arguments:

```java
// src/main/java/com/kt/MyMap.java
package com.kt;

public class MyMap<T1, T2> {
    private T1[] keys;
    private T2[] values;

    // ...
}
```

You can annotate the class with `@Template` and supply the concrete types to replace the generic type arguments with:

```java
// src/main/java/com/kt/MyMap.java
package com.kt;

import com.kt.template.Template;
import some.pkg.Foo;
import some.pkg.Bar;

@Template(
        types1 = {Double.class},
        types2 = {Foo.class}
)
public class MyMap<T1, T2> {
    private T1[] keys;
    private T2[] values;

    // ...
}
```

The annotation processor will then produce the following new class:


```java
// src/target/generated-sources/annotations/com/kt/MyMapDoubleFoo.java
package com.kt;

import some.pkg.Foo;
import some.pkg.Bar;

public class MyMapDoubleFoo {
    private Double[] keys;
    private Foo[] values;

    // ...
}
```

You can also supply multiple concrete types, in which case the annotation processor will generate more
instantiation classes, one for each entry in the <code>types{N}</code> fields (note that the length
of the arrays <code>types1</code>, <code>types2</code> etc. have to match). For example, if we
annotate <code>MyMap</code> with 

```java
@Template(
        types1 = {Double.class, String.class},
        types2 = {Foo.class, Bar.class}
)
```

then the annotation processor will produce the classes <code>MyMapDoubleFoo</code>
and <code>MyMapStringBar</code>.


## Primitives
Note that you can also supply primitive types. The annotation processor will turn

```java
// src/main/java/com/kt/MyArrayList.java
package com.kt;

import some.pkg.Foo;

@Template(
        types1 = {double.class},
        types2 = {Foo.class}
)
public class MyMap<T1, T2> {
    private T1[] keys;
    private T2[] values;

    public MyMap(int size) {
        this.keys = (T1[]) new Object[size];
        this.values = (T2[]) new Object[size];
        
        // ...
        
        T1 x = null;
    }
}
```

into the following instantiation, which, unfortunately, contains two compiler errors:

```java
// src/target/generated-sources/annotations/com/kt/MyMapDoubleFoo.java
package com.kt;

import some.pkg.Foo;

public class MyMapDoubleFoo {
    private double[] keys;
    private Foo[] values;

    public MyMap(int size) {
        this.keys = (double[]) new Object[size];  // ERROR: invalid cast
        this.values = (Foo[]) new Object[size];

        // ...

        double x = null;  // ERROR: null cannot be assigned to primitive variable
    }
}
```

Can we resolve the rror? Yes, see the next section.


## Custom replacements
For cases where simple type replacements won't do the trick there's the option to
supply custom replacements, e.g. for the compiler errors that we have encountered above.
The annotated class will look as follows:

```java
// src/main/java/com/kt/MyArrayList.java
package com.kt;

import some.pkg.Foo;

@Template(
        types1 = {double.class, Bar.class},
        types2 = {Foo.class, Foo.class},
        replacements = {
                // supply a single array composed of triplets, each with the following scheme:
                // <fully qualified type name>, <from>, <to>
                "double", "(T1[]) new Object", "new double",                         // #1
                "double", "T1 x = null", "double x = Double.NaN",                    // #2
                "some.pkg.Foo", "(T2[]) new Object[size]", "Foo.createArray(size)",  // #3
        }
)
public class MyMap<T1, T2> {
    private T1[] keys;
    private T2[] values;

    public MyMap(int size) {
        this.keys = (T1[]) new Object[size];
        this.values = (T2[]) new Object[size];

        // ...

        T1 x = null;
    }
}
```

and you will get the two following instantiations:

```java
// src/target/generated-sources/annotations/com/kt/MyMapBarFoo.java
package com.kt;

import some.pkg.Foo;

public class MyMapDoubleFoo {
    private Bar[] keys;
    private Foo[] values;

    public MyMap(int size) {
        this.keys = (Bar[]) new Object[size];
        this.values = Foo.createArray(size);  // <- custom replacement  #3

        // ...

        Bar x = null;
    }
}
```

```java
// src/target/generated-sources/annotations/com/kt/MyMapDoubleFoo.java
package com.kt;

import some.pkg.Foo;

public class MyMapDoubleFoo {
    private double[] keys;
    private Foo[] values;

    public MyMap(int size) {
        this.keys = new double[size];         // <- custom replacement  #1
        this.values = Foo.createArray(size);  // <- custom replacement  #3

        // ...

        double x = Double.NaN;                // <- custom replacement  #2
    }
}
```

Note: The fact that we have to supply the concrete types for which to apply the
custom replacements as strings (e.g. <code>"some.pkg.Foo"</code>)
is a consequence of the fact that Java annotations only accept a limited set of
parameter types. If Java were more flexible we could define custom replacements with nicer
syntax, e.g.

```java
@Template(
        // ...
        replacements = {
                Replacement(double.class, "(T1[]) new Object", "new double"),
                // ...
        }
)
```
