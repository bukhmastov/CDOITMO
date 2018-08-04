package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import androidx.annotation.NonNull;

import org.json.JSONArray;

public interface Accounts {

    void add(@NonNull final Context context, @NonNull final String login);

    void remove(@NonNull final Context context, @NonNull final String login);

    JSONArray get(@NonNull Context context);
}
