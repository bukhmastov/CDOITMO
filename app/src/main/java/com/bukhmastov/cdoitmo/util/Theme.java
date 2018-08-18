package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.Toolbar;

public interface Theme {

    String getAppTheme(final Context context);

    void updateAppTheme(final Context context);

    void applyActivityTheme(final Activity activity);

    void applyToolbarTheme(final Context context, final Toolbar toolbar);
}
