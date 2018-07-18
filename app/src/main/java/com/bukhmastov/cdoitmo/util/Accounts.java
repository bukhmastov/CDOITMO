package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.impl.AccountsImpl;

import org.json.JSONArray;

public interface Accounts {

    // future: replace with DI factory
    Accounts instance = new AccountsImpl();
    static Accounts instance() {
        return instance;
    }

    void add(@NonNull final Context context, @NonNull final String login);

    void remove(@NonNull final Context context, @NonNull final String login);

    JSONArray get(@NonNull Context context);
}
