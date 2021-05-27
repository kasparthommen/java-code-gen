package com.kt.codegen.demo.double2;

import com.kt.codegen.Generate;
import com.kt.codegen.Replace;

@Generate(name = "MyFloatList", replace = @Replace(from = "\\bdouble\\b", to = "float", regex = true))
@Generate(name = "MyLongList", replace = @Replace(from = "\\bdouble\\b", to = "long", regex = true))
public class MyDoubleList {
    private double[] array;

    MyDoubleList(int size) {
        this.array = new double[size];
    }

    // ...
}
