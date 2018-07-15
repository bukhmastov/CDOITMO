package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class CtxWrapper extends ContextWrapper {

    public CtxWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context, StoragePref storagePref, Log log) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale locale = TextUtils.getLocale(context, storagePref);
                Locale.setDefault(locale);
                Configuration config = context.getResources().getConfiguration();
                config.setLocale(locale);
                context = context.createConfigurationContext(config);
            }
        } catch (Throwable e) {
            log.exception(e);
        }
        return new CtxWrapper(context);
    }
}
