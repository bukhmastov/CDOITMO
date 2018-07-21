package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public interface Static {

    String GLITCH = "%*<@?!";

    String getUUID(Context context);

    void reLaunch(Context context);

    void hardReset(final Context context);

    void lockOrientation(Activity activity, boolean lock);

    void removeView(final View view);
}
