package com.bukhmastov.cdoitmo.event.events;

import android.os.Bundle;
import androidx.annotation.NonNull;

public class OpenActivityEvent {

    private final Class<?> activity;
    private final Bundle extras;
    private final Integer flags;
    private String identity = null;

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

    public OpenActivityEvent withIdentity(@NonNull String identity) {
        this.identity = identity;
        return this;
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

    public String getIdentity() {
        return identity;
    }

    public static class Failed {

        private final String identity;
        private final String reason;

        public Failed(String identity, String reason) {
            this.identity = identity;
            this.reason = reason;
        }

        public String getIdentity() {
            return identity;
        }

        public String getReason() {
            return reason;
        }
    }
}
