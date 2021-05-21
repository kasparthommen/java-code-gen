package com.kt.template.demo.double2;

import com.kt.template.CodeTransformer;
import com.kt.template.Replace;
import com.kt.template.ReplaceType;
import com.kt.template.Transform;

@CodeTransformer(transforms = {
    @Transform(targetName = "MyFloatList", replacements = {
        @Replace(from = "\\bdouble\\b", to = "float", replaceType = ReplaceType.REGEX)
    }),
    @Transform(targetName = "MyLongList", replacements = {
        @Replace(from = "\\bdouble\\b", to = "long", replaceType = ReplaceType.REGEX)
    })
})
public class MyDoubleList {
    private double[] array;

    MyDoubleList(int size) {
        this.array = new double[size];
    }

    // ...
}
