package com.kt.template.demo.list2;

import com.kt.template.Instantiation;
import com.kt.template.Template;

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
