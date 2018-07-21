package com.bukhmastov.cdoitmo.dialog;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;

import javax.inject.Inject;

public abstract class Dialog {

    private static final String TAG = "Dialog";
    protected final Context context;

    @Inject
    Log log;

    public Dialog(Context context) {
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
    }

    protected View inflate(@LayoutRes int layout) throws InflateException {
        if (context == null) {
            log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
