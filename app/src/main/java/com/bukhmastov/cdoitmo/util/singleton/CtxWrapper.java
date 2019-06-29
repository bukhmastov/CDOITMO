package com.bukhmastov.cdoitmo.util.singleton;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

import com.bukhmastov.cdoitmo.util.StoragePref;

import java.util.Locale;

public class CtxWrapper extends ContextWrapper {

    public CtxWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context, StoragePref storagePref) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Locale locale = StringUtils.getLocale(context, storagePref);
                Locale.setDefault(locale);
                Configuration config = context.getResources().getConfiguration();
                config.setLocale(locale);
                context = context.createConfigurationContext(config);
            }
        } catch (Throwable ignore) {
            // ignore
        }
        return new CtxWrapper(context);
    }
}
