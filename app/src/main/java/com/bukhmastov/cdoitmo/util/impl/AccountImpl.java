package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.interfaces.CallableString;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Accounts;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class AccountImpl implements Account {

    private static final String TAG = "Account";
    public boolean authorized = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    DeIfmoClient deIfmoClient;
    @Inject
    DeIfmoRestClient deIfmoRestClient;
    @Inject
    ProtocolTracker protocolTracker;
    @Inject
    Accounts accounts;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebasePerformanceProvider firebasePerformanceProvider;

    public AccountImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void login(@NonNull final Context context, @NonNull final String login, @NonNull final String password, @NonNull final String role, final boolean isNewUser, @NonNull final LoginHandler loginHandler) {
        final String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGIN);
        thread.run(() -> {
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(login);
            log.v(TAG, "login | login=", login, " | password.length()=", password.length(), " | role=", role, " | isNewUser=", isNewUser, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED, " | OFFLINE_MODE=", App.OFFLINE_MODE);
            if (login.isEmpty() || password.isEmpty()) {
                thread.runOnUI(() -> {
                    loginHandler.onFailure(context.getString(R.string.required_login_password));
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_credentials_required");
                });
                return;
            }
            if ("general".equals(login)) {
                log.w(TAG, "login | got \"general\" login that does not supported");
                thread.runOnUI(() -> {
                    loginHandler.onFailure(context.getString(R.string.wrong_login_general));
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_login_general");
                });
                return;
            }
            authorized = false;
            storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
            if (isNewUser || IS_USER_UNAUTHORIZED) {
                storage.put(context, Storage.PERMANENT, Storage.USER,"user#deifmo#login", login);
                storage.put(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", password);
                storage.put(context, Storage.PERMANENT, Storage.USER, "user#role", role);
            }
            if (IS_USER_UNAUTHORIZED) {
                thread.runOnUI(() -> {
                    authorized = true;
                    App.UNAUTHORIZED_MODE = true;
                    if (App.OFFLINE_MODE) {
                        loginHandler.onOffline();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_unauthorized_offline");
                    } else {
                        loginHandler.onSuccess();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_unauthorized");
                    }
                });
                return;
            }
            if (App.OFFLINE_MODE) {
                if (isNewUser) {
                    App.OFFLINE_MODE = false;
                } else {
                    thread.runOnUI(() -> {
                        authorized = true;
                        loginHandler.onOffline();
                        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_offline");
                    });
                    return;
                }
            }
            deIfmoClient.check(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    thread.run(() -> {
                        authorized = true;
                        accounts.add(context, login);
                        if (isNewUser) {
                            firebaseAnalyticsProvider.logBasicEvent(context, "New user authorized");
                            protocolTracker.setup(context, deIfmoRestClient, 0);
                        }
                        thread.runOnUI(() -> {
                            loginHandler.onSuccess();
                            firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
                        });
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.run(() -> {
                        final CallableString callback = text -> thread.runOnUI(() -> {
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
                                    logoutTemporarily(context, () -> {
                                        callback.call(context.getString(R.string.network_unavailable));
                                        firebasePerformanceProvider.putAttribute(trace, "state", "failed_network_unavailable");
                                    });
                                } else {
                                    authorized = true;
                                    callback.call("offline");
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_offline");
                                }
                                break;
                            default:
                            case DeIfmoClient.FAILED_TRY_AGAIN:
                            case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                            case DeIfmoClient.FAILED_SERVER_ERROR:
                                logoutTemporarily(context, () -> {
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
                                    logoutPermanently(context, login, cb);
                                } else {
                                    logoutTemporarily(context, login, cb);
                                }
                                break;
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                cb = () -> {
                                    callback.call(context.getString(R.string.invalid_login_password));
                                    firebasePerformanceProvider.putAttribute(trace, "state", "failed_credentials_failed");
                                };
                                if (isNewUser) {
                                    logoutPermanently(context, login, cb);
                                } else {
                                    logoutTemporarily(context, login, cb);
                                }
                                break;
                        }
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
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

    @Override
    public void logout(@NonNull final Context context, @NonNull final LogoutHandler logoutHandler) {
        logout(context, null, logoutHandler);
    }

    @Override
    public void logout(@NonNull final Context context, @Nullable final String login, @NonNull final LogoutHandler logoutHandler) {
        final String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGOUT);
        thread.run(() -> {
            @NonNull final String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            log.i(TAG, "logout | login=", cLogin, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED, " | OFFLINE_MODE=", App.OFFLINE_MODE);
            if ("general".equals(login)) {
                log.w(TAG, "logout | got \"general\" login that does not supported");
                thread.runOnUI(() -> {
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
                    thread.runOnUI(() -> logoutHandler.onProgress(context.getString(R.string.exiting) + "\n" + uName));
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    logoutHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void logoutPermanently(@NonNull final Context context, @Nullable final Callable callback) {
        logoutPermanently(context, null, callback);
    }

    @Override
    public void logoutPermanently(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback) {
        thread.run(() -> {
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
                    accounts.remove(context, cLogin);
                }
                storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                storage.cacheReset();
                eventBus.fire(new ClearCacheEvent());
                authorized = false;
                App.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    thread.runOnUI(callback::call);
                }
            };
            if (IS_USER_UNAUTHORIZED || IS_LOGIN_EMPTY) {
                cb.call();
            } else {
                protocolTracker.stop(context, cb);
            }
        });
    }

    @Override
    public void logoutTemporarily(@NonNull final Context context, @Nullable final Callable callback) {
        logoutTemporarily(context, null, callback);
    }

    @Override
    public void logoutTemporarily(@NonNull final Context context, @Nullable final String login, @Nullable final Callable callback) {
        thread.run(() -> {
            @NonNull final String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            final boolean IS_USER_UNAUTHORIZED = USER_UNAUTHORIZED.equals(cLogin);
            log.i(TAG, "logoutTemporarily | login=", cLogin, " | IS_USER_UNAUTHORIZED=", IS_USER_UNAUTHORIZED);
            final Callable cb = () -> {
                storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                storage.cacheReset();
                eventBus.fire(new ClearCacheEvent());
                authorized = false;
                App.UNAUTHORIZED_MODE = false;
                if (callback != null) {
                    thread.runOnUI(callback::call);
                }
            };
            if (IS_USER_UNAUTHORIZED) {
                cb.call();
            } else {
                protocolTracker.stop(context, cb);
            }
        });
    }

    @Override
    public void logoutConfirmation(@NonNull final Context context, @NonNull final Callable callback) {
        thread.runOnUI(() -> new AlertDialog.Builder(context)
                .setTitle(R.string.logout_confirmation)
                .setMessage(R.string.logout_confirmation_message)
                .setPositiveButton(R.string.do_logout, (dialogInterface, i) -> thread.runOnUI(callback::call))
                .setNegativeButton(R.string.do_cancel, null)
                .create().show());
    }

    @Override
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    @Override
    public boolean isAuthorized() {
        return authorized;
    }
}
