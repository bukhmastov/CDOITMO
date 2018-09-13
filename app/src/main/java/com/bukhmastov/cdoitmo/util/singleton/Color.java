package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

public class Color {

    public static final int INVALID_VALUE = -1;

    public static int resolve(Context context, int reference) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(reference, typedValue, true);
        TypedArray resource;
        try {
            resource = context.obtainStyledAttributes(typedValue.data, new int[]{reference});
        } catch (Resources.NotFoundException exception) {
            resource = null;
        }
        if (resource == null) {
            return INVALID_VALUE;
        }
        int color = resource.getColor(0, INVALID_VALUE);
        resource.recycle();
        return color;
    }
}
