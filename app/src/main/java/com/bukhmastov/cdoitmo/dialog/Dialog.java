package com.bukhmastov.cdoitmo.dialog;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.bukhmastov.cdoitmo.util.Log;

public abstract class Dialog {

    private static final String TAG = "Dialog";
    protected final Context context;

    public Dialog(Context context) {
        this.context = context;
    }

    protected View inflate(@LayoutRes int layout) throws InflateException {
        if (context == null) {
            Log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
