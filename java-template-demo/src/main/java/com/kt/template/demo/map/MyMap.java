package com.kt.template.demo.map;

import com.kt.template.Instantiation;
import com.kt.template.Template;

import java.time.Instant;

@Template(instantiations = {
    @Instantiation(types = {String.class, Instant.class }),  // <-- two concrete types
    // ... more instantiations
})
class MyMap<K, V> {                                          // <-- two type parameters
    private K[] keys;
    private K[] values;

    // ...
}
