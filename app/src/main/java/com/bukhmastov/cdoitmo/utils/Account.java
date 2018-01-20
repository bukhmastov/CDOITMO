package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.content.DialogInterface;
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(login);
                Log.v(TAG, "login | login=" + login + " | password.length()=" + password.length() + " | role=" + role + " | isNewUser=" + Log.lBool(isNewUser) + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED) + " | OFFLINE_MODE=" + Log.lBool(Static.OFFLINE_MODE));
                if (login.isEmpty() || password.isEmpty()) {
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loginHandler.onFailure(context.getString(R.string.required_login_password));
                        }
                    });
                    return;
                }
                if ("general".equals(login)) {
                    Log.w(TAG, "login | got \"general\" login that does not supported");
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loginHandler.onFailure(context.getString(R.string.wrong_login_general));
                        }
                    });
                    return;
                }
                Account.authorized = false;
                Storage.file.general.put(context, "users#current_login", login);
                if (isNewUser) {
                    Storage.file.perm.put(context, "user#deifmo#login", login);
                    Storage.file.perm.put(context, "user#deifmo#password", password);
                    Storage.file.perm.put(context, "user#role", role);
                }
                if (IS_USER_UNAUTHORIZED) {
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Account.authorized = true;
                            Static.UNAUTHORIZED_MODE = true;
                            if (Static.OFFLINE_MODE) {
                                loginHandler.onOffline();
                            } else {
                                loginHandler.onSuccess();
                            }
                        }
                    });
                    return;
                }
                if (Static.OFFLINE_MODE) {
                    if (isNewUser) {
                        Static.OFFLINE_MODE = false;
                    } else {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Account.authorized = true;
                                loginHandler.onOffline();
                            }
                        });
                        return;
                    }
                }
                DeIfmoClient.check(context, new ResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Client.Headers headers, String response) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                Account.authorized = true;
                                Account.List.push(context, login);
                                if (isNewUser) {
                                    FirebaseAnalyticsProvider.logBasicEvent(context, "New user authorized");
                                    Static.protocolChangesTrackSetup(context, 0);
                                }
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loginHandler.onSuccess();
                                    }
                                });
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                final String text;
                                switch (state) {
                                    case DeIfmoClient.FAILED_OFFLINE:
                                        if (isNewUser) {
                                            Account.logoutTemporarily(context, null);
                                            text = context.getString(R.string.network_unavailable);
                                        } else {
                                            Account.authorized = true;
                                            text = "offline";
                                        }
                                        break;
                                    default:
                                    case DeIfmoClient.FAILED_TRY_AGAIN:
                                    case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                                    case DeIfmoClient.FAILED_INTERRUPTED:
                                    case DeIfmoClient.FAILED_SERVER_ERROR:
                                        Account.logoutTemporarily(context, null);
                                        text = context.getString(R.string.auth_failed) + (state == DeIfmoClient.FAILED_SERVER_ERROR ? ". " + DeIfmoClient.getFailureMessage(context, statusCode) : "");
                                        break;
                                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                        Account.logoutPermanently(context, login, null);
                                        text = context.getString(R.string.required_login_password);
                                        break;
                                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                        Account.logoutPermanently(context, login, null);
                                        text = context.getString(R.string.invalid_login_password);
                                        break;
                                }
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if ("offline".equals(text)) {
                                            loginHandler.onOffline();
                                        } else {
                                            loginHandler.onFailure(text);
                                        }
                                    }
                                });
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                            }
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        loginHandler.onNewRequest(request);
                    }
                });
            }
        });
    }

    public static void logout(@NonNull final Context context, @NonNull final LogoutHandler logoutHandler) {
        logout(context, null, logoutHandler);
    }
    public static void logout(@NonNull final Context context, @Nullable final String login, @NonNull final LogoutHandler logoutHandler) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                @NonNull final String cLogin = login != null ? login : Storage.file.general.get(context, "users#current_login");
                final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
                Log.i(TAG, "logout | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED) + " | OFFLINE_MODE=" + Log.lBool(Static.OFFLINE_MODE));
                if ("general".equals(login)) {
                    Log.w(TAG, "logout | got \"general\" login that does not supported");
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logoutHandler.onFailure(context.getString(R.string.wrong_login_general));
                        }
                    });
                    return;
                }
                if (IS_USER_UNAUTHORIZED || Static.OFFLINE_MODE || cLogin.isEmpty()) {
                    logoutPermanently(context, cLogin, new Static.SimpleCallback() {
                        @Override
                        public void onCall() {
                            logoutHandler.onSuccess();
                        }
                    });
                    return;
                }
                Storage.file.general.put(context, "users#current_login", cLogin);
                final String uName = Storage.file.perm.get(context, "user#name");
                DeIfmoClient.get(context, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Log.v(TAG, "logout | onSuccess");
                        logoutPermanently(context, cLogin, new Static.SimpleCallback() {
                            @Override
                            public void onCall() {
                                logoutHandler.onSuccess();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Log.v(TAG, "logout | onFailure | statusCode=" + statusCode + " | state=" + state);
                        logoutPermanently(context, cLogin, new Static.SimpleCallback() {
                            @Override
                            public void onCall() {
                                logoutHandler.onSuccess();
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logoutHandler.onProgress(context.getString(R.string.exiting) + "\n" + uName);
                            }
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        logoutHandler.onNewRequest(request);
                    }
                });
            }
        });
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final Static.SimpleCallback callback) {
        logoutPermanently(context, null, callback);
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final String login, @Nullable final Static.SimpleCallback callback) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                @NonNull final String cLogin = login != null ? login : Storage.file.general.get(context, "users#current_login");
                final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
                final boolean IS_LOGIN_EMPTY = cLogin.isEmpty();
                Log.v(TAG, "logoutPermanently | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED));
                if (!IS_LOGIN_EMPTY) {
                    Storage.file.general.put(context, "users#current_login", cLogin);
                }
                final Static.SimpleCallback cb = new Static.SimpleCallback() {
                    @Override
                    public void onCall() {
                        if (!IS_USER_UNAUTHORIZED && !IS_LOGIN_EMPTY) {
                            Storage.file.all.clear(context);
                            Account.List.remove(context, cLogin);
                        }
                        Storage.file.general.delete(context, "users#current_login");
                        Storage.cache.reset();
                        Account.authorized = false;
                        Static.UNAUTHORIZED_MODE = false;
                        if (callback != null) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onCall();
                                }
                            });
                        }
                    }
                };
                if (IS_USER_UNAUTHORIZED || IS_LOGIN_EMPTY) {
                    cb.onCall();
                } else {
                    new ProtocolTracker(context).stop(cb);
                }
            }
        });
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final Static.SimpleCallback callback) {
        logoutTemporarily(context, null, callback);
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final String login, @Nullable final Static.SimpleCallback callback) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                @NonNull final String cLogin = login != null ? login : Storage.file.general.get(context, "users#current_login");
                final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
                Log.i(TAG, "logoutTemporarily | login=" + cLogin + " | IS_USER_UNAUTHORIZED=" + Log.lBool(IS_USER_UNAUTHORIZED));
                final Static.SimpleCallback cb = new Static.SimpleCallback() {
                    @Override
                    public void onCall() {
                        Storage.file.general.delete(context, "users#current_login");
                        Storage.cache.reset();
                        Account.authorized = false;
                        Static.UNAUTHORIZED_MODE = false;
                        if (callback != null) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onCall();
                                }
                            });
                        }
                    }
                };
                if (IS_USER_UNAUTHORIZED) {
                    cb.onCall();
                } else {
                    new ProtocolTracker(context).stop(cb);
                }

            }
        });
    }
    public static void logoutConfirmation(@NonNull final Context context, @NonNull final Static.SimpleCallback callback) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.logout_confirmation)
                        .setMessage(R.string.logout_confirmation_message)
                        .setPositiveButton(R.string.do_logout, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Static.T.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onCall();
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.do_cancel, null)
                        .create().show();
            }
        });
    }

    public static class List {
        private static final String TAG = Account.TAG + ".List";
        public static void push(@NonNull final Context context, @NonNull final String login) {
            if (USER_UNAUTHORIZED.equals(login)) return;
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.v(TAG, "push | login=" + login);
                        boolean isNewAuthorization = true;
                        // save login on top of the list of authorized users
                        JSONArray list = Static.string2jsonArray(Storage.file.general.get(context, "users#list", ""));
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
                        Storage.file.general.put(context, "users#list", accounts.toString());
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
                }
            });
        }
        public static void remove(@NonNull final Context context, @NonNull final String login) {
            if (USER_UNAUTHORIZED.equals(login)) return;
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.v(TAG, "remove | login=" + login);
                        // remove login from the list of authorized users
                        JSONArray list = Static.string2jsonArray(Storage.file.general.get(context, "users#list", ""));
                        for (int i = 0; i < list.length(); i++) {
                            if (list.getString(i).equals(login)) {
                                list.remove(i);
                                break;
                            }
                        }
                        Storage.file.general.put(context, "users#list", list.toString());
                        // track statistics
                        FirebaseAnalyticsProvider.logEvent(
                                context,
                                FirebaseAnalyticsProvider.Event.LOGOUT,
                                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, list.length())
                        );
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            });
        }
        public static JSONArray get(@NonNull Context context) {
            Log.v(TAG, "get");
            try {
                return Static.string2jsonArray(Storage.file.general.get(context, "users#list", ""));
            } catch (Exception e) {
                Static.error(e);
                return new JSONArray();
            }
        }
    }
}
