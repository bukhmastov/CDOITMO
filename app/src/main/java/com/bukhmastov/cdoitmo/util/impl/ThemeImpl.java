package com.bukhmastov.cdoitmo.util.impl;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.view.dialog.ThemeDialog;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

public class ThemeImpl implements Theme {

    private String appTheme = null;

    @Inject
    StoragePref storagePref;
    @Inject
    Time time;

    public ThemeImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public String getAppTheme(Context context) {
        if (appTheme == null) {
            updateAppTheme(context);
        }
        return appTheme;
    }

    @Override
    public void updateAppTheme(Context context) {
        appTheme = ThemeDialog.getTheme(context, storagePref, time);
    }

    @Override
    public void applyActivityTheme(Activity activity) {
        if (activity == null) {
            return;
        }
        switch (getAppTheme(activity)) {
            case "default": activity.setTheme(getDefaultAppTheme(activity)); break;
            case "dark": activity.setTheme(R.style.AppTheme_Dark); break;
            case "white": activity.setTheme(R.style.AppTheme_White); break;
            case "black": activity.setTheme(R.style.AppTheme_Black); break;
            case "light": default: activity.setTheme(R.style.AppTheme); break;
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
            case "default": toolbarContext.setTheme(getDefaultAppToolbarTheme(context)); break;
            case "dark": toolbarContext.setTheme(R.style.AppTheme_Toolbar_Dark); break;
            case "white": toolbarContext.setTheme(R.style.AppTheme_Toolbar_White); break;
            case "black": toolbarContext.setTheme(R.style.AppTheme_Toolbar_Black); break;
            case "light": default: toolbarContext.setTheme(R.style.AppTheme_Toolbar); break;
        }
    }

    private int getDefaultAppTheme(Context context) {
        if (isSystemDarkMode(context)) {
            return R.style.AppTheme_Dark;
        } else {
            return R.style.AppTheme;
        }
    }

    private int getDefaultAppToolbarTheme(Context context) {
        if (isSystemDarkMode(context)) {
            return R.style.AppTheme_Toolbar_Dark;
        } else {
            return R.style.AppTheme_Toolbar;
        }
    }

    private boolean isSystemDarkMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES: return true;
                case Configuration.UI_MODE_NIGHT_NO: return false;
            }
        }
        return false;
    }
}
