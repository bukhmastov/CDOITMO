package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.interfaces.CallableString;

//TODO interface - impl
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

    //@Inject
    //TODO interface - impl: remove static
    private static Log log = Log.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static Storage storage = Storage.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static StoragePref storagePref = StoragePref.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static DeIfmoClient deIfmoClient = DeIfmoClient.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static DeIfmoRestClient deIfmoRestClient = DeIfmoRestClient.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();
    //@Inject
    //TODO interface - impl: remove static
    private static FirebasePerformanceProvider firebasePerformanceProvider = FirebasePerformanceProvider.instance();

    public static void login(@NonNull final Context context, @NonNull final String login, @NonNull final String password, @NonNull final String role, final boolean isNewUser, @NonNull final LoginHandler loginHandler) {
        final String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGIN);
        Thread.run(() -> {
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(login);
            log.v(TAG, "login | login=", login, " | password.length()=", password.length(), " | role=", role, " | isNewUser=", isNewUser, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED, " | OFFLINE_MODE=", App.OFFLINE_MODE);
            if (login.isEmpty() || password.isEmpty()) {
                Thread.runOnUI(() -> {
                    loginHandler.onFailure(context.getString(R.string.required_login_password));
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_credentials_required");
                });
                return;
            }
            if ("general".equals(login)) {
                log.w(TAG, "login | got \"general\" login that does not supported");
                Thread.runOnUI(() -> {
                    loginHandler.onFailure(context.getString(R.string.wrong_login_general));
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_login_general");
                });
                return;
            }
            Account.authorized = false;
            storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
            if (isNewUser || IS_USER_UNAUTHORIZED) {
                storage.put(context, Storage.PERMANENT, Storage.USER,"user#deifmo#login", login);
                storage.put(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", password);
                storage.put(context, Storage.PERMANENT, Storage.USER, "user#role", role);
            }
            if (IS_USER_UNAUTHORIZED) {
                Thread.runOnUI(() -> {
                    Account.authorized = true;
                    App.UNAUTHORIZED_MODE = true;
                    if (App.OFFLINE_MODE) {
                        loginHandler.onOffline();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_unauthorized_offline");
                    } else {
                        loginHandler.onSuccess();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_unauthorized");
                    }
                    return;
                });
            }
            if (App.OFFLINE_MODE) {
                if (isNewUser) {
                    App.OFFLINE_MODE = false;
                } else {
                    Thread.runOnUI(() -> {
                        Account.authorized = true;
                        loginHandler.onOffline();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_offline");
                    });
                    return;
                }
            }
            deIfmoClient.check(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    Thread.run(() -> {
                        Account.authorized = true;
                        Accounts.push(context, login);
                        if (isNewUser) {
                            firebaseAnalyticsProvider.logBasicEvent(context, "New user authorized");
                            ProtocolTracker.setup(context, deIfmoRestClient, storagePref, log, 0);
                        }
                        Thread.runOnUI(() -> {
                            loginHandler.onSuccess();
                            firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
                        });
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    Thread.run(() -> {
                        final CallableString callback = text -> Thread.runOnUI(() -> {
                            if ("offline".equals(text)) {
                                loginHandler.onOffline();
                            } else {
                                loginHandler.onFailure(text);
                            }
                            firebasePerformanceProvider.stopTrace(trace);
                        });
                        Callable cb;
                        switch (state) {
                            case DeIfmoClient.FAILED_OFFLINE:
                                if (isNewUser) {
                                    Account.logoutTemporarily(context, () -> {
                                        callback.call(context.getString(R.string.network_unavailable));
                                        firebasePerformanceProvider.putAttribute(trace, "state", "failed_network_unavailable");
                                    });
                                } else {
                                    Account.authorized = true;
                                    callback.call("offline");
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_offline");
                                }
                                break;
                            default:
                            case DeIfmoClient.FAILED_TRY_AGAIN:
                            case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                            case DeIfmoClient.FAILED_SERVER_ERROR:
                                Account.logoutTemporarily(context, () -> {
                                    callback.call(context.getString(R.string.auth_failed) + (state == DeIfmoClient.FAILED_SERVER_ERROR ? ". " + DeIfmoClient.getFailureMessage(context, statusCode) : ""));
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_auth");
                                });
                                break;
                            case DeIfmoClient.FAILED_INTERRUPTED:
                                loginHandler.onInterrupted();
                                break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                cb = () -> {
                                    callback.call(context.getString(R.string.required_login_password));
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_credentials_required");
                                };
                                if (isNewUser) {
                                    Account.logoutPermanently(context, login, cb);
                                } else {
                                    Account.logoutTemporarily(context, login, cb);
                                }
                                break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                cb = () -> {
                                    callback.call(context.getString(R.string.invalid_login_password));
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_credentials_failed");
                                };
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
                    Thread.runOnUI(() -> {
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
        final String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGOUT);
        Thread.run(() -> {
            @NonNull final String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            log.i(TAG, "logout | login=", cLogin, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED, " | OFFLINE_MODE=", App.OFFLINE_MODE);
            if ("general".equals(login)) {
                log.w(TAG, "logout | got \"general\" login that does not supported");
                Thread.runOnUI(() -> {
                    logoutHandler.onFailure(context.getString(R.string.wrong_login_general));
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_login_general");
                });
                return;
            }
            if (IS_USER_UNAUTHORIZED || App.OFFLINE_MODE || cLogin.isEmpty()) {
                logoutPermanently(context, cLogin, () -> {
                    logoutHandler.onSuccess();
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_local");
                });
                return;
            }
            storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", cLogin);
            final String uName = storage.get(context, Storage.PERMANENT, Storage.USER, "user#name");
            deIfmoClient.get(context, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    log.v(TAG, "logout | onSuccess");
                    logoutPermanently(context, cLogin, () -> {
                        logoutHandler.onSuccess();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    log.v(TAG, "logout | onFailure | statusCode=", statusCode, " | state=", state);
                    logoutPermanently(context, cLogin, () -> {
                        logoutHandler.onSuccess();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
                    });
                }
                @Override
                public void onProgress(final int state) {
                    Thread.runOnUI(() -> logoutHandler.onProgress(context.getString(R.string.exiting) + "\n" + uName));
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    logoutHandler.onNewRequest(request);
                }
            });
        });
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final Callable callback) {
        logoutPermanently(context, null, callback);
    }
    public static void logoutPermanently(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback) {
        Thread.run(() -> {
            @NonNull final String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            final boolean IS_LOGIN_EMPTY = cLogin.isEmpty();
            log.v(TAG, "logoutPermanently | login=", cLogin, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED);
            if (!IS_LOGIN_EMPTY) {
                storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", cLogin);
            }
            final Callable cb = () -> {
                if (!IS_USER_UNAUTHORIZED && !IS_LOGIN_EMPTY) {
                    storage.clear(context, null, Storage.USER);
                    Accounts.remove(context, cLogin);
                }
                storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                storage.cacheReset();
                Account.authorized = false;
                App.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    Thread.runOnUI(callback::call);
                }
            };
            if (IS_USER_UNAUTHORIZED || IS_LOGIN_EMPTY) {
                cb.call();
            } else {
                new ProtocolTracker(context).stop(cb);
            }
        });
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final Callable callback) {
        logoutTemporarily(context, null, callback);
    }
    public static void logoutTemporarily(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback) {
        Thread.run(() -> {
            @NonNull final String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            log.i(TAG, "logoutTemporarily | login=", cLogin, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED);
            final Callable cb = () -> {
                storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                storage.cacheReset();
                Account.authorized = false;
                App.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    Thread.runOnUI(callback::call);
                }
            };
            if (IS_USER_UNAUTHORIZED) {
                cb.call();
            } else {
                new ProtocolTracker(context).stop(cb);
            }
        });
    }
    public static void logoutConfirmation(@NonNull final Context context, @NonNull final Callable callback) {
        Thread.runOnUI(() -> new AlertDialog.Builder(context)
                .setTitle(R.string.logout_confirmation)
                .setMessage(R.string.logout_confirmation_message)
                .setPositiveButton(R.string.do_logout, (dialogInterface, i) -> Thread.runOnUI(callback::call))
                .setNegativeButton(R.string.do_cancel, null)
                .create().show());
    }

}
