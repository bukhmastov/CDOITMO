package com.bukhmastov.cdoitmo.event.events;

import android.content.Intent;
import androidx.annotation.NonNull;

public class OpenIntentEvent {

    private final Intent intent;
    private String identity = null;

    public OpenIntentEvent(Intent intent) {
        this.intent = intent;
    }

    public OpenIntentEvent withIdentity(@NonNull String identity) {
        this.identity = identity;
        return this;
    }

    public Intent getIntent() {
        return intent;
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
