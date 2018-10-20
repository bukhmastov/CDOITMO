package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.network.model.Client;

public interface Account {

    String USER_UNAUTHORIZED = "unauthorized";

    void login(@NonNull final Context context, @NonNull final String login, @NonNull final String password, @NonNull final String role, final boolean isNewUser, @NonNull final LoginHandler loginHandler);

    void logout(@NonNull final Context context, @NonNull final LogoutHandler logoutHandler);

    void logout(@NonNull final Context context, @Nullable final String login, @NonNull final LogoutHandler logoutHandler);

    void logoutPermanently(@NonNull final Context context, @Nullable final Callable callback);

    void logoutPermanently(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback);

    void logoutTemporarily(@NonNull final Context context, @Nullable final Callable callback);

    void logoutTemporarily(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback);

    void logoutConfirmation(@NonNull final Context context, @NonNull final Callable callback);

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
