package com.kt.codegen.demo.list2;

import com.kt.codegen.Instantiation;
import com.kt.codegen.Template;

@Template(instantiations = {
    @Instantiation(types = {String.class}),
})
class MyList<T> {
    private T[] array;

    MyList(int size) {
        this.array = (T[]) new Object[size];
    }

    T get(int index) {
        return array[index];
    }
}
