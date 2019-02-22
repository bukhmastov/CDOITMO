package com.bukhmastov.cdoitmo.util.impl;

import android.app.AlertDialog;
import android.content.Context;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.function.Callable;
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
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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
    public void login(@NonNull Context context, @NonNull String login, @NonNull String password,
                      @NonNull String role, boolean isNewUser, @NonNull LoginHandler handler) {
        thread.assertNotUI();
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGIN);
        boolean isUserUnauthorized = USER_UNAUTHORIZED.equals(login);
        log.v(TAG, "login | login=", login, " | password.length()=", password.length(),
                " | role=", role, " | isNewUser=", isNewUser, " | isUserUnauthorized=",
                isUserUnauthorized, " | OFFLINE_MODE=", App.OFFLINE_MODE);
        if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
            loginInvokeFailed(context, handler, isNewUser, trace,
                    "failed_credentials_required", R.string.required_login_password);
            return;
        }
        if ("general".equals(login)) {
            log.w(TAG, "login | got \"general\" login that does not supported");
            loginInvokeFailed(context, handler, isNewUser, trace,
                    "failed_login_general", R.string.wrong_login_general);
            return;
        }
        authorized = false;
        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
        if (isNewUser || isUserUnauthorized) {
            storage.put(context, Storage.PERMANENT, Storage.USER,"user#deifmo#login", login);
            storage.put(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", password);
            storage.put(context, Storage.PERMANENT, Storage.USER, "user#role", role);
        }
        App.UNAUTHORIZED_MODE = isUserUnauthorized;
        if (App.UNAUTHORIZED_MODE) {
            if (App.OFFLINE_MODE) {
                loginInvokeOffline(context, handler, trace, "success_unauthorized_offline", login, false);
            } else {
                loginInvokeSuccess(context, handler, trace, login, isNewUser, false);
            }
            return;
        }
        if (App.OFFLINE_MODE) {
            if (isNewUser) {
                App.OFFLINE_MODE = false;
            } else {
                loginInvokeOffline(context, handler, trace, "success_offline", login, true);
                return;
            }
        }
        if (!deIfmoClient.isAuthExpiredByJsessionId(context)) {
            loginInvokeSuccess(context, handler, trace, login, isNewUser, true);
            return;
        }
        if (!Client.isOnline(context)) {
            loginInvokeFailed(context, handler, isNewUser, trace,
                    "failed_network_unavailable", R.string.network_unavailable);
            return;
        }
        thread.runOnUI(() -> handler.onProgress(context.getString(R.string.auth_check)));
        deIfmoClient.authorize(context, new ResponseHandler() {
            @Override
            public void onSuccess(int code, Client.Headers headers, String response) {
                if (isNewUser) {
                    firebaseAnalyticsProvider.logBasicEvent(context, "New user authorized");
                }
                loginInvokeSuccess(context, handler, trace, login, isNewUser, true);
            }
            @Override
            public void onProgress(int state) {
                if (state == DeIfmoClient.STATE_AUTHORIZED) {
                    thread.runOnUI(() -> handler.onProgress(context.getString(R.string.authorized)));
                } else {
                    thread.runOnUI(() -> handler.onProgress(context.getString(R.string.authorization)));
                }
            }
            @Override
            public void onFailure(int code, Client.Headers headers, int state) {
                switch (state) {
                    case DeIfmoClient.FAILED_OFFLINE: {
                        if (isNewUser) {
                            logoutTemporarily(context, () -> {
                                loginInvokeFailed(context, handler, isNewUser, trace,
                                        "failed_network_unavailable", R.string.network_unavailable);
                            });
                        } else {
                            loginInvokeOffline(context, handler, trace, "failed_offline", login, true);
                        }
                        break;
                    }
                    default:
                    case DeIfmoClient.FAILED_TRY_AGAIN:
                    case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                    case DeIfmoClient.FAILED_SERVER_ERROR: {
                        logoutTemporarily(context, () -> {
                            String message = context.getString(R.string.auth_failed);
                            if (state == DeIfmoClient.FAILED_SERVER_ERROR) {
                                message += ". " + DeIfmoClient.getFailureMessage(context, code);
                            }
                            loginInvokeFailed(context, handler, isNewUser, trace,
                                    "failed_auth", message);
                        });
                        break;
                    }
                    case DeIfmoClient.FAILED_INTERRUPTED: {
                        handler.onInterrupted();
                        break;
                    }
                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: {
                        if (isNewUser) {
                            logoutPermanently(context, login, () -> {
                                loginInvokeFailed(context, handler, isNewUser, trace,
                                        "failed_credentials_required", R.string.required_login_password);
                            });
                        } else {
                            logoutTemporarily(context, login, () -> {
                                loginInvokeFailed(context, handler, isNewUser, trace,
                                        "failed_credentials_required", R.string.required_login_password);
                            });
                        }
                        break;
                    }
                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: {
                        if (isNewUser) {
                            logoutPermanently(context, login, () -> {
                                loginInvokeFailed(context, handler, isNewUser, trace,
                                        "failed_credentials_failed", R.string.invalid_login_password);
                            });
                        } else {
                            logoutTemporarily(context, login, () -> {
                                loginInvokeFailed(context, handler, isNewUser, trace,
                                        "failed_credentials_failed", R.string.invalid_login_password);
                            });
                        }
                        break;
                    }
                }
            }
            @Override
            public void onNewRequest(Client.Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    private void loginInvokeFailed(Context context, LoginHandler handler, boolean isNewUser,
                                   String trace, String state, @StringRes int message) {
        loginInvokeFailed(context, handler, isNewUser, trace, state, context.getString(message));
    }

    private void loginInvokeFailed(Context context, LoginHandler handler, boolean isNewUser,
                                   String trace, String state, String message) {
        if (isNewUser) {
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.LOGIN_FAILED,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, state)
            );
        }
        thread.runOnUI(() -> handler.onFailure(message));
        firebasePerformanceProvider.putAttributeAndStop(trace, "state", state);
    }

    private void loginInvokeOffline(Context context, LoginHandler handler, String trace, String traceValue,
                                    String login, boolean isAuthorized) {
        authorized = true;
        thread.runOnUI(handler::onOffline);
        firebasePerformanceProvider.putAttributeAndStop(trace, "state", traceValue);
        if (isAuthorized) {
            accounts.add(context, login);
        }
    }

    private void loginInvokeSuccess(Context context, LoginHandler handler, String trace, String login,
                                    boolean isNewUser, boolean isAuthorized) {
        authorized = true;
        thread.runOnUI(handler::onSuccess);
        firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
        if (isAuthorized) {
            accounts.add(context, login);
        }
        if (isNewUser && isAuthorized) {
            thread.standalone(() -> protocolTracker.setup(context, deIfmoRestClient));
        }
    }

    @Override
    public void logout(@NonNull Context context, @NonNull LogoutHandler logoutHandler) {
        logout(context, null, logoutHandler);
    }

    @Override
    public void logout(@NonNull Context context, @Nullable String cLogin, @NonNull LogoutHandler logoutHandler) {
        thread.assertNotUI();
        String trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.LOGOUT);
        String login = cLogin != null ? cLogin : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
        boolean isUserUnauthorized = USER_UNAUTHORIZED.equals(login);
        log.i(TAG, "logout | login=", login, " | isUserUnauthorized=", isUserUnauthorized, " | OFFLINE_MODE=", App.OFFLINE_MODE);
        if ("general".equals(login)) {
            log.w(TAG, "logout | got \"general\" login that does not supported");
            thread.runOnUI(() -> {
                logoutHandler.onFailure(context.getString(R.string.wrong_login_general));
                firebasePerformanceProvider.putAttributeAndStop(trace, "state", "failed_login_general");
            });
            return;
        }
        if (isUserUnauthorized || App.OFFLINE_MODE || login.isEmpty()) {
            logoutPermanently(context, login, () -> {
                logoutHandler.onSuccess();
                firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success_local");
            });
            return;
        }
        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
        String uName = storage.get(context, Storage.PERMANENT, Storage.USER, "user#name");
        deIfmoClient.get(context, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new ResponseHandler() {
            @Override
            public void onSuccess(int code, Client.Headers headers, String response) {
                log.v(TAG, "logout | onSuccess");
                logoutPermanently(context, login, () -> {
                    logoutHandler.onSuccess();
                    firebasePerformanceProvider.putAttributeAndStop(trace, "state", "success");
                });
            }
            @Override
            public void onFailure(int code, Client.Headers headers, int state) {
                log.v(TAG, "logout | onFailure | code=", code, " | state=", state);
                logoutPermanently(context, login, () -> {
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
    }

    @Override
    public void logoutPermanently(@NonNull Context context, @Nullable Callable callback) {
        logoutPermanently(context, null, callback);
    }

    @Override
    public void logoutPermanently(@NonNull Context context, @Nullable String login, @Nullable Callable callback) {
        thread.assertNotUI();
        @NonNull String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
        boolean isUserUnauthorized = USER_UNAUTHORIZED.equals(cLogin);
        boolean isLoginEmpty = StringUtils.isBlank(cLogin);
        log.v(TAG, "logoutPermanently | login=", cLogin, " | isUserUnauthorized=", isUserUnauthorized);
        if (!isLoginEmpty) {
            storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", cLogin);
        }
        Callable cb = () -> {
            if (!isUserUnauthorized && !isLoginEmpty) {
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
        if (isUserUnauthorized || isLoginEmpty) {
            cb.call();
        } else {
            protocolTracker.stop(context, cb);
        }
    }

    @Override
    public void logoutTemporarily(@NonNull Context context, @Nullable Callable callback) {
        logoutTemporarily(context, null, callback);
    }

    @Override
    public void logoutTemporarily(@NonNull Context context, @Nullable String login, @Nullable Callable callback) {
        thread.assertNotUI();
        @NonNull String cLogin = login != null ? login : storage.get(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
        boolean isUserUnauthorized = USER_UNAUTHORIZED.equals(cLogin);
        log.i(TAG, "logoutTemporarily | login=", cLogin, " | isUserUnauthorized=", isUserUnauthorized);
        Callable cb = () -> {
            storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            storage.cacheReset();
            eventBus.fire(new ClearCacheEvent());
            authorized = false;
            App.UNAUTHORIZED_MODE = false;
            if (callback != null) {
                thread.runOnUI(callback::call);
            }
        };
        if (isUserUnauthorized) {
            cb.call();
        } else {
            protocolTracker.stop(context, cb);
        }
    }

    @Override
    public void logoutConfirmation(@NonNull Context context, @NonNull Callable callback) {
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
