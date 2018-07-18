package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.bukhmastov.cdoitmo.util.impl.ThemeImpl;

public interface Theme {

    // future: replace with DI factory
    Theme instance = new ThemeImpl();
    static Theme instance() {
        return instance;
    }

    String getAppTheme(final Context context);

    void updateAppTheme(final Context context);

    void applyActivityTheme(final Activity activity);

    void applyToolbarTheme(final Context context, final Toolbar toolbar);
}
