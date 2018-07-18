package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.bukhmastov.cdoitmo.util.impl.StaticImpl;

public interface Static {

    // future: replace with DI factory
    Static instance = new StaticImpl();
    static Static instance() {
        return instance;
    }

    String GLITCH = "%*<@?!";

    String getUUID(Context context);

    void reLaunch(Context context);

    void hardReset(final Context context);

    void lockOrientation(Activity activity, boolean lock);

    void removeView(final View view);
}
