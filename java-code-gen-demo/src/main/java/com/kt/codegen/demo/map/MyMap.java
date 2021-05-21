package com.kt.codegen.demo.map;

import com.kt.codegen.Instantiation;
import com.kt.codegen.Template;

import java.time.Instant;

@Template(instantiations = {
    @Instantiation(types = {String.class, Instant.class }),  // <-- two concrete types
    // ... more instantiations
})
class MyMap<K, V> {                                          // <-- two type parameters
    // ...
}
