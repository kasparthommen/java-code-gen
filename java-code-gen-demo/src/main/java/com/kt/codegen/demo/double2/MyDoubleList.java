package com.kt.codegen.demo.double2;

import com.kt.codegen.Replace;
import com.kt.codegen.Transform;

@Transform(target = "MyFloatList", replace = @Replace(from = "\\bdouble\\b", to = "float", regex = true))
@Transform(target = "MyLongList", replace = @Replace(from = "\\bdouble\\b", to = "long", regex = true))
public class MyDoubleList {
    private double[] array;

    MyDoubleList(int size) {
        this.array = new double[size];
    }

    // ...
}
