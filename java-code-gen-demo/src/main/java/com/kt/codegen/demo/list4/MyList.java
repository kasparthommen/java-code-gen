package com.kt.codegen.demo.list4;

import com.kt.codegen.Instantiate;
import com.kt.codegen.Replace;
import com.kt.codegen.Template;

@Template({
    @Instantiate(value = String.class,
                 replace = @Replace(from = "(T[]) new Object[size]", to = "new String[size]")),
    @Instantiate(value = double.class,
                 replace = @Replace(from = "(T[]) new Object[size]", to = "new double[size]"))
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
