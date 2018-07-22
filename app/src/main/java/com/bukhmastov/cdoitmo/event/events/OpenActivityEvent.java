package com.bukhmastov.cdoitmo.event.events;

import android.os.Bundle;

public class OpenActivityEvent {

    private final Class<?> activity;
    private final Bundle extras;
    private final Integer flags;

    public OpenActivityEvent(Class<?> activity) {
        this(activity, null, null);
    }

    public OpenActivityEvent(Class<?> activity, Bundle extras) {
        this(activity, extras, null);
    }

    public OpenActivityEvent(Class<?> activity, Integer flags) {
        this(activity, null, flags);
    }

    public OpenActivityEvent(Class<?> activity, Bundle extras, Integer flags) {
        this.activity = activity;
        this.extras = extras;
        this.flags = flags;
    }

    public Class<?> getActivity() {
        return activity;
    }

    public Bundle getExtras() {
        return extras;
    }

    public Integer getFlags() {
        return flags;
    }
}
