package com.kt.codegen.demo.double2;

import com.kt.codegen.Derive;
import com.kt.codegen.Replace;

@Derive(name = "MyFloatList", replace = @Replace(from = "\\bdouble\\b", to = "float", regex = true))
@Derive(name = "MyLongList", replace = @Replace(from = "\\bdouble\\b", to = "long", regex = true))
public class MyDoubleList {
    private double[] array;

    MyDoubleList(int size) {
        this.array = new double[size];
    }

    // ...
}
