package com.kt.codegen.demo.map;

import com.kt.codegen.Instantiate;

import java.time.Instant;

@Instantiate({String.class, Instant.class})  // <-- two concrete types
class MyMap<K, V> {                          // <-- two type parameters
    private K[] keys;
    private V[] values;

    // ...
}
