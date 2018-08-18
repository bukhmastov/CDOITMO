package com.bukhmastov.cdoitmo.object.preference;

import android.content.Context;

public class PreferenceImitation extends Preference {

    private final boolean allowApplyDefaultValue;

    public PreferenceImitation(String key, Object defaultValue, boolean allowApplyDefaultValue) {
        super(key, defaultValue, 0);
        this.allowApplyDefaultValue = allowApplyDefaultValue;
    }

    @Override
    public void applyDefaultValue(Context context) {
        if (!allowApplyDefaultValue) {
            return;
        }
        super.applyDefaultValue(context);
    }
}
