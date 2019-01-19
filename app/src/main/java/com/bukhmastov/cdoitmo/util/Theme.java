package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public interface Theme {

    String getAppTheme(Context context);

    void updateAppTheme(Context context);

    void applyActivityTheme(Activity activity);

    void applyToolbarTheme(Context context, View toolbar);
}
