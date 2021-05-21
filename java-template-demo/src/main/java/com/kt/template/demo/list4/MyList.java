package com.kt.template.demo.list4;

import com.kt.template.Instantiation;
import com.kt.template.Replace;
import com.kt.template.Template;

@Template(instantiations = {
    @Instantiation(
        types = {String.class},
        replacements = {
            @Replace(from = "(T[]) new Object[size]", to = "new String[size]")
        }
    ),
    @Instantiation(
        types = {double.class},
        replacements = {
            @Replace(from = "(T[]) new Object[size]", to = "new double[size]")
        }
    )
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
