package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class CtxWrapper extends ContextWrapper {

    public CtxWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale locale = Static.getLocale(context);
                Locale.setDefault(locale);
                Configuration config = context.getResources().getConfiguration();
                config.setLocale(locale);
                context = context.createConfigurationContext(config);
            }
        } catch (Throwable e) {
            Static.error(e);
        }
        return new CtxWrapper(context);
    }

}
