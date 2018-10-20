package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.model.user.UsersList;

public interface Accounts {

    void add(@NonNull final Context context, @NonNull final String login);

    void remove(@NonNull final Context context, @NonNull final String login);

    @NonNull UsersList get(@NonNull Context context);
}
