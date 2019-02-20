package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.network.model.Client;

public interface Account {

    String USER_UNAUTHORIZED = "unauthorized";
    String ROLE_STUDENT = "student";

    void login(@NonNull Context context, @NonNull String login, @NonNull String password,
               @NonNull String role, boolean isNewUser, @NonNull LoginHandler loginHandler);

    void logout(@NonNull Context context, @NonNull LogoutHandler logoutHandler);

    void logout(@NonNull Context context, @Nullable String login, @NonNull LogoutHandler logoutHandler);

    void logoutPermanently(@NonNull Context context, @Nullable Callable callback);

    void logoutPermanently(@NonNull Context context, @Nullable String login, @Nullable Callable callback);

    void logoutTemporarily(@NonNull Context context, @Nullable Callable callback);

    void logoutTemporarily(@NonNull Context context, @Nullable String login, @Nullable Callable callback);

    void logoutConfirmation(@NonNull Context context, @NonNull Callable callback);

    void setAuthorized(boolean authorized);

    boolean isAuthorized();

    interface LoginHandler {
        void onSuccess();
        void onOffline();
        void onInterrupted();
        void onFailure(final String text);
        void onProgress(final String text);
        void onNewRequest(final Client.Request request);
    }

    interface LogoutHandler {
        void onSuccess();
        void onFailure(final String text);
        void onProgress(final String text);
        void onNewRequest(final Client.Request request);
    }
}
