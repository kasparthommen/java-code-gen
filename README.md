# Java Template
### Introduction
An approach to emulate C++ templates. Works by generating class instantiations with concrete types using a simple annotation. Helps avoiding virtual method calls.

### Example
Say you have the following generic class with two type arguments:

```java
// src/main/java/com/kt/Klass.java

package com.kt;

public class Klass<T1 extends Number, T2> {
    private T1 t1;
    private T2 t2;

    // ...
}
```

You can annotate the class with `@Template` and supply the concrete types to replace the generic type arguments with:

```java
// src/main/java/com/kt/Klass.java

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
```

The annotation process will then generate four classes, two of which are as follows:


```java
// src/target/generated-sources/annotations/com/kt/KlassDoubleFoo.java

package com.kt;

import some.pkg.Foo;
import some.pkg.Bar;

public class KlassDoubleFoo {
    private Double t1;
    private Foo t2;

    // ...
}
```



```java
// src/target/generated-sources/annotations/com/kt/KlassStringBar.java

package com.kt;

import some.pkg.Foo;
import some.pkg.Bar;

public class KlassStringBar {
    private String t1;
    private Bar t2;

    // ...
}
```

Note that we have declared the first generic type as `T1 extends Number` but we want it to be instantiated as `Double` and `String`, the latter **not** extending `Number` and thus violating the generic class specification. The `@Template` annotation ignores this and happily generates `String` instantiations anyway. This may lead to compiler errors, e.g. if `Klass` calls `t1.doubleValue()` somewhere.
