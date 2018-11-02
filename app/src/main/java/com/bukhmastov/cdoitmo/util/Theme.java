package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toolbar;

public interface Theme {

    String getAppTheme(Context context);

    void updateAppTheme(Context context);

    void applyActivityTheme(Activity activity);

    void applyToolbarTheme(Context context, Toolbar toolbar);
}
