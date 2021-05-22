package com.kt.codegen.demo.list2;

import com.kt.codegen.Instantiate;
import com.kt.codegen.Template;

@Template(@Instantiate(String.class))
class MyList<T> {
    private T[] array;

    MyList(int size) {
        this.array = (T[]) new Object[size];
    }

    T get(int index) {
        return array[index];
    }
}
