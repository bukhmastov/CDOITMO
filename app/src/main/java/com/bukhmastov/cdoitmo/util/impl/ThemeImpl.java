package com.bukhmastov.cdoitmo.util.impl;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.dialog.ThemeDialog;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

public class ThemeImpl implements Theme {

    private String app_theme = null;

    @Inject
    StoragePref storagePref;
    @Inject
    Time time;

    public ThemeImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public String getAppTheme(Context context) {
        if (app_theme == null) {
            updateAppTheme(context);
        }
        return app_theme;
    }

    @Override
    public void updateAppTheme(Context context) {
        app_theme = ThemeDialog.getTheme(context, storagePref, time);
    }

    @Override
    public void applyActivityTheme(Activity activity) {
        if (activity == null) {
            return;
        }
        switch (getAppTheme(activity)) {
            case "light":
            default: activity.setTheme(R.style.AppTheme); break;
            case "dark": activity.setTheme(R.style.AppTheme_Dark); break;
            case "white": activity.setTheme(R.style.AppTheme_White); break;
            case "black": activity.setTheme(R.style.AppTheme_Black); break;
        }
    }

    @Override
    public void applyToolbarTheme(Context context, View toolbar) {
        if (context == null || toolbar == null) {
            return;
        }
        Context toolbarContext = toolbar.getContext();
        if (toolbarContext == null) {
            return;
        }
        switch (getAppTheme(context)) {
            case "light":
            default: toolbarContext.setTheme(R.style.AppTheme_Toolbar); break;
            case "dark": toolbarContext.setTheme(R.style.AppTheme_Toolbar_Dark); break;
            case "white": toolbarContext.setTheme(R.style.AppTheme_Toolbar_White); break;
            case "black": toolbarContext.setTheme(R.style.AppTheme_Toolbar_Black); break;
        }
    }
}
