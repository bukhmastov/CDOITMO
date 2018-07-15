package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.dialog.ThemeDialog;

//TODO interface - impl
public class Theme {

    private static String app_theme = null;

    //@Inject
    //TODO interface - impl: remove static
    private static StoragePref storagePref = StoragePref.instance();

    public static String getAppTheme(final Context context) {
        if (app_theme == null) {
            updateAppTheme(context);
        }
        return app_theme;
    }

    public static void updateAppTheme(final Context context) {
        app_theme = ThemeDialog.getTheme(context, storagePref);
    }

    public static void applyActivityTheme(final Activity activity) {
        if (activity != null) {
            switch (getAppTheme(activity)) {
                case "light":
                default: activity.setTheme(R.style.AppTheme); break;
                case "dark": activity.setTheme(R.style.AppTheme_Dark); break;
                case "white": activity.setTheme(R.style.AppTheme_White); break;
                case "black": activity.setTheme(R.style.AppTheme_Black); break;
            }
        }
    }

    public static void applyToolbarTheme(final Context context, final Toolbar toolbar) {
        if (toolbar != null) {
            Context toolbar_context = toolbar.getContext();
            if (toolbar_context != null) {
                switch (getAppTheme(context)) {
                    case "light":
                    default: toolbar_context.setTheme(R.style.AppTheme_Toolbar); break;
                    case "dark": toolbar_context.setTheme(R.style.AppTheme_Toolbar_Dark); break;
                    case "white": toolbar_context.setTheme(R.style.AppTheme_Toolbar_White); break;
                    case "black": toolbar_context.setTheme(R.style.AppTheme_Toolbar_Black); break;
                }
            }
        }
    }
}
