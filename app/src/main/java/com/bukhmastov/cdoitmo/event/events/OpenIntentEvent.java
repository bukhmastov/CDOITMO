package com.bukhmastov.cdoitmo.event.events;

import android.content.Intent;

public class OpenIntentEvent {

    private final Intent intent;

    public OpenIntentEvent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }
}
