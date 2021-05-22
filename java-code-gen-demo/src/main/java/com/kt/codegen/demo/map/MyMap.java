package com.kt.codegen.demo.map;

import com.kt.codegen.Instantiate;
import com.kt.codegen.Template;

import java.time.Instant;

@Template(@Instantiate({String.class, Instant.class }))  // <-- two concrete types
class MyMap<K, V> {                                      // <-- two type parameters
    // ...
}
