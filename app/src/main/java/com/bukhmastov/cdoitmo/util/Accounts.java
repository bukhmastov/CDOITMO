package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.model.user.UsersList;

public interface Accounts {

    void add(@NonNull Context context, @NonNull String login);

    void remove(@NonNull Context context, @NonNull String login);

    @NonNull UsersList get(@NonNull Context context);
}
