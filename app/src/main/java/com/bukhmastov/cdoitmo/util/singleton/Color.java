package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;
import android.util.SparseArray;
import android.util.TypedValue;

public class Color {

    public static int resolve(Context context, int reference) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(reference, typedValue, true);
        return context.obtainStyledAttributes(typedValue.data, new int[]{reference}).getColor(0, -1);
    }
}
