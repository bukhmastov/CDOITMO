package com.bukhmastov.cdoitmo.factory;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.module.AppModule;

public class AppComponentProvider {

    private static AppComponent appComponent = null;
    private static Context appContext = null;

    public static AppComponent getComponent(Context context) {
        appContext = context;
        build();
        return appComponent;
    }

    public static AppComponent getComponent() {
        build();
        return appComponent;
    }

    private static void build() {
        if (appComponent == null) {
            appComponent = DaggerAppComponent
                    .builder()
                    .appModule(new AppModule(appContext))
                    .build();
        }
    }
}
