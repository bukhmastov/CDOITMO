package com.bukhmastov.cdoitmo.factory;

public class AppComponentProvider {

    private static AppComponent appComponent = null;

    public static AppComponent getComponent() {
        if (appComponent == null) {
            appComponent = DaggerAppComponent.builder().build();
        }
        return appComponent;
    }
}
