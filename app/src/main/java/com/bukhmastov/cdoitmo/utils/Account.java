package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;

import org.json.JSONArray;

public class Account {

    private static final String TAG = "Account";

    public static final String USER_UNAUTHORIZED = "unauthorized";
    public static boolean authorized = false;

    public interface LoginHandler {
        void onSuccess();
        void onOffline();
        void onInterrupted();
        void onFailure(final String text);
        void onProgress(final String text);
        void onNewRequest(final Client.Request request);
    }
    public interface LogoutHandler {
        void onSuccess();
        void onFailure(final String text);
        void onProgress(final String text);
        void onNewRequest(final Client.Request request);
    }

    public static void login(@NonNull final Context context, @NonNull final String login, @NonNull final String password, @NonNull final String role, final boolean isNewUser, @NonNull final LoginHandler loginHandler) {
        Static.T.runThread(() -> {
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(login);
            Log.v(TAG, "login | login=" + login + " | password.length()=" + password.length() + " | role=" + role + " | isNewUser=" + Log.lBool(isNewUser) + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED) + " | OFFLINE_MODE=" + Log.lBool(Static.OFFLINE_MODE));
            if (login.isEmpty() || password.isEmpty()) {
                Static.T.runOnUiThread(() -> loginHandler.onFailure(context.getString(R.string.required_login_password)));
                return;
            }
            if ("general".equals(login)) {
                Log.w(TAG, "login | got \"general\" login that does not supported");
                Static.T.runOnUiThread(() -> loginHandler.onFailure(context.getString(R.string.wrong_login_general)));
                return;
            }
            Account.authorized = false;
            Storage.file.general.perm.put(context, "users#current_login", login);
            if (isNewUser || IS_USER_UNAUTHORIZED) {
                Storage.file.perm.put(context, "user#deifmo#login", login);
                Storage.file.perm.put(context, "user#deifmo#password", password);
                Storage.file.perm.put(context, "user#role", role);
            }
            if (IS_USER_UNAUTHORIZED) {
                Static.T.runOnUiThread(() -> {
                    Account.authorized = true;
                    Static.UNAUTHORIZED_MODE = true;
                    if (Static.OFFLINE_MODE) {
                        loginHandler.onOffline();
                    } else {
                        loginHandler.onSuccess();
                    }
                });
                return;
            }
            if (Static.OFFLINE_MODE) {
                if (isNewUser) {
                    Static.OFFLINE_MODE = false;
                } else {
                    Static.T.runOnUiThread(() -> {
                        Account.authorized = true;
                        loginHandler.onOffline();
                    });
                    return;
                }
            }
            DeIfmoClient.check(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    Static.T.runThread(() -> {
                        Account.authorized = true;
                        List.push(context, login);
                        if (isNewUser) {
                            FirebaseAnalyticsProvider.logBasicEvent(context, "New user authorized");
                            Static.protocolChangesTrackSetup(context, 0);
                        }
                        Static.T.runOnUiThread(loginHandler::onSuccess);
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    Static.T.runThread(() -> {
                        final Static.StringCallback callback = text -> Static.T.runOnUiThread(() -> {
                            if ("offline".equals(text)) {
                                loginHandler.onOffline();
                            } else {
                                loginHandler.onFailure(text);
                            }
                        });
                        Static.SimpleCallback cb;
                        switch (state) {
                            case DeIfmoClient.FAILED_OFFLINE:
                                if (isNewUser) {
                                    Account.logoutTemporarily(context, () -> callback.onCall(context.getString(R.string.network_unavailable)));
                                } else {
                                    Account.authorized = true;
                                    callback.onCall("offline");
                                }
                                break;
                            default:
                            case DeIfmoClient.FAILED_TRY_AGAIN:
                            case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                            case DeIfmoClient.FAILED_SERVER_ERROR:
                                Account.logoutTemporarily(context, () -> callback.onCall(context.getString(R.string.auth_failed) + (state == DeIfmoClient.FAILED_SERVER_ERROR ? ". " + DeIfmoClient.getFailureMessage(context, statusCode) : "")));
                                break;
                            case DeIfmoClient.FAILED_INTERRUPTED:
                                loginHandler.onInterrupted();
                                break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                cb = () -> callback.onCall(context.getString(R.string.required_login_password));
                                if (isNewUser) {
                                    Account.logoutPermanently(context, login, cb);
                                } else {
                                    Account.logoutTemporarily(context, login, cb);
                                }
                                break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                cb = () -> callback.onCall(context.getString(R.string.invalid_login_password));
                                if (isNewUser) {
                                    Account.logoutPermanently(context, login, cb);
                                } else {
                                    Account.logoutTemporarily(context, login, cb);
                                }
                                break;
                        }
                    });
                }
                @Override
                public void onProgress(final int state) {
                    Static.T.runOnUiThread(() -> {
                        switch (state) {
                            default:
                            case DeIfmoClient.STATE_HANDLING:
                            case DeIfmoClient.STATE_CHECKING:
                                loginHandler.onProgress(context.getString(R.string.auth_check));
                                break;
                            case DeIfmoClient.STATE_AUTHORIZATION:
                                loginHandler.onProgress(context.getString(R.string.authorization));
                                break;
                            case DeIfmoClient.STATE_AUTHORIZED:
                                loginHandler.onProgress(context.getString(R.string.authorized));
                                break;
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    loginHandler.onNewRequest(request);
                }
            });
        });
    }

    public static void logout(@NonNull final Context context, @NonNull final LogoutHandler logoutHandler) {
        logout(context, null, logoutHandler);
    }
    public static void logout(@NonNull final Context context, @Nullable final String login, @NonNull final LogoutHandler logoutHandler) {
        Static.T.runThread(() -> {
            @NonNull final String cLogin = login != null ? login : Storage.file.general.perm.get(context, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            Log.i(TAG, "logout | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED) + " | OFFLINE_MODE=" + Log.lBool(Static.OFFLINE_MODE));
            if ("general".equals(login)) {
                Log.w(TAG, "logout | got \"general\" login that does not supported");
                Static.T.runOnUiThread(() -> logoutHandler.onFailure(context.getString(R.string.wrong_login_general)));
                return;
            }
            if (IS_USER_UNAUTHORIZED || Static.OFFLINE_MODE || cLogin.isEmpty()) {
                logoutPermanently(context, cLogin, logoutHandler::onSuccess);
                return;
            }
            Storage.file.general.perm.put(context, "users#current_login", cLogin);
            final String uName = Storage.file.perm.get(context, "user#name");
            DeIfmoClient.get(context, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    Log.v(TAG, "logout | onSuccess");
                    logoutPermanently(context, cLogin, logoutHandler::onSuccess);
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    Log.v(TAG, "logout | onFailure | statusCode=" + statusCode + " | state=" + state);
                    logoutPermanently(context, cLogin, logoutHandler::onSuccess);
                }
                @Override
                public void onProgress(final int state) {
                    Static.T.runOnUiThread(() -> logoutHandler.onProgress(context.getString(R.string.exiting) + "\n" + uName));
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    logoutHandler.onNewRequest(request);
                }
            });
        });
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final Static.SimpleCallback callback) {
        logoutPermanently(context, null, callback);
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final String login, @Nullable final Static.SimpleCallback callback) {
        Static.T.runThread(() -> {
            @NonNull final String cLogin = login != null ? login : Storage.file.general.perm.get(context, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            final boolean IS_LOGIN_EMPTY = cLogin.isEmpty();
            Log.v(TAG, "logoutPermanently | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED));
            if (!IS_LOGIN_EMPTY) {
                Storage.file.general.perm.put(context, "users#current_login", cLogin);
            }
            final Static.SimpleCallback cb = () -> {
                if (!IS_USER_UNAUTHORIZED && !IS_LOGIN_EMPTY) {
                    Storage.file.all.clear(context);
                    List.remove(context, cLogin);
                }
                Storage.file.general.perm.delete(context, "users#current_login");
                Storage.cache.reset();
                Account.authorized = false;
                Static.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    Static.T.runOnUiThread(callback::onCall);
                }
            };
            if (IS_USER_UNAUTHORIZED || IS_LOGIN_EMPTY) {
                cb.onCall();
            } else {
                new ProtocolTracker(context).stop(cb);
            }
        });
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final Static.SimpleCallback callback) {
        logoutTemporarily(context, null, callback);
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final String login, @Nullable final Static.SimpleCallback callback) {
        Static.T.runThread(() -> {
            @NonNull final String cLogin = login != null ? login : Storage.file.general.perm.get(context, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            Log.i(TAG, "logoutTemporarily | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED));
            final Static.SimpleCallback cb = () -> {
                Storage.file.general.perm.delete(context, "users#current_login");
                Storage.cache.reset();
                Account.authorized = false;
                Static.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    Static.T.runOnUiThread(callback::onCall);
                }
            };
            if (IS_USER_UNAUTHORIZED) {
                cb.onCall();
            } else {
                new ProtocolTracker(context).stop(cb);
            }

        });
    }
    public static void logoutConfirmation(@NonNull final Context context, @NonNull final Static.SimpleCallback callback) {
        Static.T.runOnUiThread(() -> new AlertDialog.Builder(context)
                .setTitle(R.string.logout_confirmation)
                .setMessage(R.string.logout_confirmation_message)
                .setPositiveButton(R.string.do_logout, (dialogInterface, i) -> Static.T.runOnUiThread(callback::onCall))
                .setNegativeButton(R.string.do_cancel, null)
                .create().show());
    }

    public static class List {
        private static final String TAG = Account.TAG + ".List";
        public static void push(@NonNull final Context context, @NonNull final String login) {
            if (USER_UNAUTHORIZED.equals(login)) return;
            Static.T.runThread(() -> {
                try {
                    Log.v(TAG, "push | login=" + login);
                    boolean isNewAuthorization = true;
                    // save login on top of the list of authorized users
                    JSONArray list = get(context);
                    JSONArray accounts = new JSONArray();
                    accounts.put(login);
                    for (int i = 0; i < list.length(); i++) {
                        String entry = list.getString(i);
                        if (entry.equals(login)) {
                            isNewAuthorization = false;
                        } else {
                            accounts.put(entry);
                        }
                    }
                    Storage.file.general.perm.put(context, "users#list", accounts.toString());
                    // track statistics
                    Bundle bundle;
                    bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, accounts.length());
                    bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_NEW, isNewAuthorization ? "new" : "old", bundle);
                    FirebaseAnalyticsProvider.logEvent(
                            context,
                            FirebaseAnalyticsProvider.Event.LOGIN,
                            bundle
                    );
                } catch (Exception e) {
                    Static.error(e);
                }
            });
        }
        public static void remove(@NonNull final Context context, @NonNull final String login) {
            if (USER_UNAUTHORIZED.equals(login)) return;
            Static.T.runThread(() -> {
                try {
                    Log.v(TAG, "remove | login=" + login);
                    // remove login from the list of authorized users
                    JSONArray list = get(context);
                    for (int i = 0; i < list.length(); i++) {
                        if (list.getString(i).equals(login)) {
                            list.remove(i);
                            break;
                        }
                    }
                    Storage.file.general.perm.put(context, "users#list", list.toString());
                    // track statistics
                    FirebaseAnalyticsProvider.logEvent(
                            context,
                            FirebaseAnalyticsProvider.Event.LOGOUT,
                            FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, list.length())
                    );
                } catch (Exception e) {
                    Static.error(e);
                }
            });
        }
        public static JSONArray get(@NonNull Context context) {
            try {
                Log.v(TAG, "get");
                try {
                    return Static.string2jsonArray(Storage.file.general.perm.get(context, "users#list", ""));
                } catch (Exception e) {
                    return new JSONArray();
                }
            } catch (Exception e) {
                Static.error(e);
                return new JSONArray();
            }
        }
    }
}
