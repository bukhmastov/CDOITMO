package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.IntroducingActivity;
import com.bukhmastov.cdoitmo.activity.LoginActivity;
import com.bukhmastov.cdoitmo.activity.presenter.LoginActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.MainActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;
import com.bukhmastov.cdoitmo.model.parser.UserDataParser;
import com.bukhmastov.cdoitmo.model.user.UserData;
import com.bukhmastov.cdoitmo.model.user.UserWeek;
import com.bukhmastov.cdoitmo.model.user.UsersList;
import com.bukhmastov.cdoitmo.model.user.isu.IsuUserData;
import com.bukhmastov.cdoitmo.model.user.isu.IsuUserDataGroup;
import com.bukhmastov.cdoitmo.model.week.IsuWeek;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Accounts;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import dagger.Lazy;

public class LoginActivityPresenterImpl implements LoginActivityPresenter {

    private static final String TAG = "LoginActivity";
    private LoginActivity activity = null;
    private Client.Request requestHandle = null;
    private boolean autoLogout = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    Account account;
    @Inject
    Accounts accounts;
    @Inject
    DeIfmoClient deIfmoClient;
    @Inject
    IsuRestClient isuRestClient;
    @Inject
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Static staticUtil;
    @Inject
    Theme theme;
    @Inject
    com.bukhmastov.cdoitmo.util.TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebaseConfigProvider firebaseConfigProvider;
    @Inject
    Lazy<Time> time;

    public LoginActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onAutoLogoutChanged(MainActivityEvent.AutoLogoutChangedEvent event) {
        autoLogout = event.isAutoLogout();
    }

    @Override
    public void setActivity(@NonNull LoginActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.runOnUI(() -> {
            log.i(TAG, "Activity created");
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            // Show introducing activity
            if (App.showIntroducingActivity) {
                App.showIntroducingActivity = false;
                eventBus.fire(new OpenActivityEvent(IntroducingActivity.class));
            }
            // setup toolbar
            Toolbar toolbar = activity.findViewById(R.id.toolbar_login);
            if (toolbar != null) {
                theme.applyToolbarTheme(activity, toolbar);
                toolbar.setTitle("");
                TypedArray resource = activity.obtainStyledAttributes(new int[]{R.attr.ic_toolbar_security});
                toolbar.setLogo(resource.getDrawable(0));
                resource.recycle();
                activity.setSupportActionBar(toolbar);
            }
            displayRemoteMessage();
            if (autoLogout) {
                autoLogout = false;
                route(LoginActivity.SIGNAL_LOGOUT);
            } else {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    route(intent.getIntExtra("state", LoginActivity.SIGNAL_LOGIN));
                } else {
                    route(LoginActivity.SIGNAL_LOGIN);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        thread.runOnUI(() -> {
            if (menu == null) {
                return;
            }
            MenuItem about = menu.findItem(R.id.action_about);
            if (about != null) {
                about.setVisible(true);
                about.setOnMenuItemClickListener(item -> {
                    thread.runOnUI(() -> {
                        Bundle extras = new Bundle();
                        extras.putBoolean(ConnectedActivity.ACTIVITY_WITH_MENU, false);
                        activity.openActivity(AboutFragment.class, extras);
                    });
                    return false;
                });
            }
        });
    }

    private void route(int signal) {
        thread.run(() -> {
            log.i(TAG, "route | signal=", signal);
            App.OFFLINE_MODE = false;
            App.UNAUTHORIZED_MODE = false;
            switch (signal) {
                case LoginActivity.SIGNAL_LOGIN: {
                    show();
                    break;
                }
                case LoginActivity.SIGNAL_RECONNECT: {
                    account.setAuthorized(false);
                    show();
                    break;
                }
                case LoginActivity.SIGNAL_GO_OFFLINE: {
                    App.OFFLINE_MODE = true;
                    show();
                    break;
                }
                case LoginActivity.SIGNAL_CHANGE_ACCOUNT: {
                    String current_login = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                    if (!current_login.isEmpty()) {
                        account.logoutTemporarily(activity, this::show);
                    } else {
                        show();
                    }
                    break;
                }
                case LoginActivity.SIGNAL_DO_CLEAN_AUTH: {
                    String current_login = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                    if (!current_login.isEmpty()) {
                        storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies");
                    }
                    show();
                    break;
                }
                case LoginActivity.SIGNAL_LOGOUT: {
                    String current_login = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                    if (!current_login.isEmpty()) {
                        logout(current_login);
                    } else {
                        show();
                    }
                    break;
                }
                case LoginActivity.SIGNAL_CREDENTIALS_REQUIRED: {
                    String current_login = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                    if (!current_login.isEmpty()) {
                        storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies");
                        account.logoutTemporarily(activity, () -> {
                            notificationMessage.snackBar(activity, activity.getString(R.string.required_login_password));
                            show();
                        });
                    }
                    show();
                    break;
                }
                case LoginActivity.SIGNAL_CREDENTIALS_FAILED: {
                    String current_login = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                    if (!current_login.isEmpty()) {
                        storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#cookies");
                        storage.delete(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password");
                        account.logoutTemporarily(activity, () -> {
                            notificationMessage.snackBar(activity, activity.getString(R.string.invalid_login_password));
                            show();
                        });
                    }
                    show();
                    break;
                }
                default: {
                    log.wtf(TAG, "route | unsupported signal: signal=", signal, " | going to use signal=SIGNAL_LOGIN");
                    route(LoginActivity.SIGNAL_LOGIN);
                    break;
                }
            }
        });
    }

    private void show() {
        thread.run(() -> {
            log.v(TAG, "show");
            firebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGIN_REQUIRED);
            String currentLogin = storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", null);
            if (StringUtils.isNotBlank(currentLogin)) {
                String cLogin = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login", null);
                String cPassword = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password", null);
                if (StringUtils.isNotBlank(cLogin) && StringUtils.isNotBlank(cPassword)) {
                    String cRole = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#role", "");
                    login(cLogin, cPassword, cRole, false);
                    return;
                }
            }
            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            appendNewUserView(container);
            appendAllUsersView(container);
            appendAnonUserView(container);
            thread.runOnUI(() -> activity.draw(container));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void appendNewUserView(ViewGroup container) {
        ViewGroup newUserTile = (ViewGroup) activity.inflate(R.layout.layout_login_new_user_tile);
        EditText inputLogin = newUserTile.findViewById(R.id.input_login);
        EditText inputPassword = newUserTile.findViewById(R.id.input_password);
        newUserTile.findViewById(R.id.login).setOnClickListener(v -> {
            log.v(TAG, "new_user_tile login clicked");
            String login = "";
            String password = "";
            if (inputLogin != null) {
                login = inputLogin.getText().toString();
            }
            if (inputPassword != null) {
                password = inputPassword.getText().toString();
            }
            // we support only 'student' role at the moment
            login(login, password, Account.ROLE_STUDENT, true);
        });
        newUserTile.findViewById(R.id.help).setOnClickListener(view -> {
            firebaseAnalyticsProvider.logBasicEvent(activity, "Help with login clicked");
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_help)
                    .setTitle(R.string.auth_help_0)
                    .setMessage(
                            activity.getString(R.string.auth_help_1) + "\n" +
                                    activity.getString(R.string.auth_help_2) + "\n\n" +
                                    activity.getString(R.string.auth_help_3) + "\n" +
                                    activity.getString(R.string.auth_help_4) + "\n\n" +
                                    activity.getString(R.string.auth_help_5) + "\n" +
                                    activity.getString(R.string.auth_help_6) + "\n\n" +
                                    activity.getString(R.string.auth_help_7)
                    )
                    .setNegativeButton(R.string.close, null)
                    .create().show();
        });
        container.addView(newUserTile);
    }

    private void appendAllUsersView(ViewGroup container) {
        UsersList acs = accounts.get(activity);
        for (String acLogin : CollectionUtils.emptyIfNull(acs.getLogins())) {
            try {
                // unique situation, we need to grab info about accounts in which we are not logged in
                // danger zone begins
                log.v(TAG, "show | account in accounts | login=", acLogin);
                storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", acLogin);
                String login = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login");
                String password = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password");
                String role = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#role");
                String name = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name").trim();
                storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                // danger zone ends
                ViewGroup userTile = (ViewGroup) activity.inflate(R.layout.layout_login_user_tile);
                View nameView = userTile.findViewById(R.id.name);
                View descView = userTile.findViewById(R.id.desc);
                String desc = "";
                if (!login.isEmpty()) {
                    desc += login;
                }
                switch (role) {
                    case Account.ROLE_STUDENT: {
                        desc += desc.isEmpty() ? activity.getString(R.string.student) : " (" + activity.getString(R.string.student) + ")";
                        break;
                    }
                    default: {
                        desc += desc.isEmpty() ? role : " (" + role + ")";
                        break;
                    }
                }
                if (!name.isEmpty()) {
                    if (nameView != null) {
                        ((TextView) nameView).setText(name);
                    }
                    if (descView != null) {
                        ((TextView) descView).setText(desc);
                    }
                } else {
                    if (nameView != null) {
                        ((TextView) nameView).setText(desc);
                    }
                    if (descView != null) {
                        staticUtil.removeView(descView);
                    }
                }
                userTile.findViewById(R.id.auth).setOnClickListener(v -> {
                    log.v(TAG, "user_tile login clicked");
                    login(login, password, role, false);
                });
                userTile.findViewById(R.id.expand_auth_menu).setOnClickListener(view -> {
                    log.v(TAG, "user_tile expand_auth_menu clicked");
                    PopupMenu popup = new PopupMenu(activity, view);
                    popup.inflate(R.menu.auth_expanded_menu);
                    popup.setOnMenuItemClickListener(item -> {
                        log.v(TAG, "auth_expanded_menu | popup.MenuItem clicked | ", item.getTitle().toString());
                        switch (item.getItemId()) {
                            case R.id.offline: {
                                storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
                                route(LoginActivity.SIGNAL_GO_OFFLINE);
                                break;
                            }
                            case R.id.clean_auth: {
                                storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
                                route(LoginActivity.SIGNAL_DO_CLEAN_AUTH);
                                break;
                            }
                            case R.id.logout: {
                                account.logoutConfirmation(activity, () -> logout(login));
                                break;
                            }
                            case R.id.change_password: {
                                thread.runOnUI(() -> {
                                    if (activity.isFinishing() || activity.isDestroyed()) {
                                        return;
                                    }
                                    View layout = activity.inflate(R.layout.preference_dialog_input);
                                    EditText editText = layout.findViewById(R.id.edittext);
                                    TextView message = layout.findViewById(R.id.message);
                                    editText.setHint(R.string.new_password);
                                    message.setText(activity.getString(R.string.change_password_message).replace("%login%", login));
                                    new AlertDialog.Builder(activity)
                                            .setTitle(R.string.change_password_title)
                                            .setView(layout)
                                            .setPositiveButton(R.string.accept, (dialog, which) -> {
                                                try {
                                                    final String value = editText.getText().toString().trim();
                                                    if (!value.isEmpty()) {
                                                        thread.run(() -> {
                                                            // unique situation, we need to modify account info in which we are not logged in
                                                            // danger zone begins
                                                            storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", acLogin);
                                                            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#password", value);
                                                            storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
                                                            // danger zone ends
                                                            notificationMessage.snackBar(activity, activity.getString(R.string.password_changed));
                                                        });
                                                    }
                                                } catch (Exception e) {
                                                    log.exception(e);
                                                }
                                            })
                                            .setNegativeButton(R.string.cancel, null)
                                            .create().show();
                                }, throwable -> {
                                    log.exception(throwable);
                                });
                                break;
                            }
                        }
                        popup.dismiss();
                        return true;
                    });
                    popup.show();
                });
                container.addView(userTile);
            } catch (Exception e) {
                log.exception(e);
            }
        }
    }

    private void appendAnonUserView(ViewGroup container) {
        ViewGroup anonymousUserTile = (ViewGroup) activity.inflate(R.layout.layout_login_anonymous_user_tile);
        EditText inputGroup = anonymousUserTile.findViewById(R.id.input_group);
        // grab current groups of anon user
        // not really danger zone begins
        storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", Account.USER_UNAUTHORIZED);
        inputGroup.setText(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#groups", ""));
        storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
        // not really danger zone ends
        anonymousUserTile.findViewById(R.id.login).setOnClickListener(view -> {
            log.v(TAG, "anonymous_user_tile login clicked");
            String group = textUtils.prettifyGroupNumber(inputGroup.getText().toString());
            String[] groups = group.split(",\\s|\\s|,");
            // set anon user info
            // not really danger zone begins
            storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", Account.USER_UNAUTHORIZED);
            String g = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group");
            boolean gFound = false;
            for (String g1 : groups) {
                if (g1.equals(g)) {
                    gFound = true;
                    break;
                }
            }
            if (!gFound) {
                g = groups.length > 0 ? groups[0] : "";
            }
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#name", activity.getString(R.string.anonymous));
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#group", g);
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#groups", TextUtils.join(", ", groups));
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#avatar", "");
            storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login");
            // not really danger zone ends
            login(Account.USER_UNAUTHORIZED, Account.USER_UNAUTHORIZED, "anonymous", false);
        });
        anonymousUserTile.findViewById(R.id.expand_auth_menu).setOnClickListener(view -> {
            log.v(TAG, "anonymous_user_tile expand_auth_menu clicked");
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.auth_anonymous_expanded_menu);
            popup.setOnMenuItemClickListener(item -> {
                log.v(TAG, "auth_expanded_menu | popup.MenuItem clicked | ", item.getTitle().toString());
                switch (item.getItemId()) {
                    case R.id.offline: {
                        storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", Account.USER_UNAUTHORIZED);
                        route(LoginActivity.SIGNAL_GO_OFFLINE);
                        break;
                    }
                }
                popup.dismiss();
                return true;
            });
            popup.show();
        });
        anonymousUserTile.findViewById(R.id.info).setOnClickListener(view -> {
            firebaseAnalyticsProvider.logBasicEvent(activity, "Help with anonymous login clicked");
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_help)
                    .setTitle(R.string.anonymous_login)
                    .setMessage(
                            activity.getString(R.string.anonymous_login_info_1) + "\n" +
                                    activity.getString(R.string.anonymous_login_info_2) + "\n\n" +
                                    activity.getString(R.string.anonymous_login_info_3) + "\n" +
                                    activity.getString(R.string.anonymous_login_info_4)
                    )
                    .setNegativeButton(R.string.close, null)
                    .create().show();
        });
        container.addView(anonymousUserTile);
    }

    private void login(String login, String password, String role, boolean isNewUser) {
        log.v(TAG, "login | login=", login, " | role=", role, " | isNewUser=", isNewUser);
        staticUtil.lockOrientation(activity, true);
        eventBus.fire(new ClearCacheEvent());
        account.login(activity, login, password, role, isNewUser, new Account.LoginHandler() {
            @Override
            public void onSuccess() {
                log.v(TAG, "login | onSuccess");
                loginSetupInformation(this, isNewUser, () -> {
                    activity.finish();
                    staticUtil.lockOrientation(activity, false);
                });
            }
            @Override
            public void onOffline() {
                log.v(TAG, "login | onOffline");
                activity.finish();
                staticUtil.lockOrientation(activity, false);
            }
            @Override
            public void onInterrupted() {
                log.v(TAG, "login | onInterrupted");
                firebaseAnalyticsProvider.logBasicEvent(activity, "login interrupted");
                storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "users#current_login", login);
                route(LoginActivity.SIGNAL_GO_OFFLINE);
                staticUtil.lockOrientation(activity, false);
            }
            @Override
            public void onFailure(String text) {
                log.v(TAG, "login | onFailure | text=", text);
                notificationMessage.snackBar(activity, text);
                show();
                staticUtil.lockOrientation(activity, false);
            }
            @Override
            public void onProgress(String text) {
                log.v(TAG, "login | onProgress | text=", text);
                activity.draw(R.layout.state_auth);
                if (isNewUser) {
                    View interruptAuthContainer = activity.findViewById(R.id.interrupt_auth_container);
                    if (interruptAuthContainer != null) {
                        interruptAuthContainer.setVisibility(View.GONE);
                    }
                } else {
                    View interrupt_auth = activity.findViewById(R.id.interrupt_auth);
                    if (interrupt_auth != null) {
                        interrupt_auth.setOnClickListener(v -> {
                            log.v(TAG, "login | onProgress | login interrupt clicked");
                            if (requestHandle != null && requestHandle.cancel()) {
                                log.v(TAG, "login | onProgress | login interrupted");
                            }
                        });
                    }
                }
                TextView loading_message = activity.findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(text);
                }
            }
            @Override
            public void onNewRequest(Client.Request request) {
                log.v(TAG, "login | onNewRequest");
                requestHandle = request;
            }
        });
    }

    private void loginSetupInformation(Account.LoginHandler handler, boolean isNewUser, ThrowingRunnable onDone) {
        if (isNewUser) {
            thread.runOnUI(() -> handler.onProgress(activity.getString(R.string.data_initializing)));
            loginSetupInformation(() -> thread.runOnUI(onDone));
        } else {
            loginSetupInformation(() -> {});
            thread.runOnUI(onDone);
        }
    }

    private void loginSetupInformation(Runnable onDone) {
        thread.standalone(() -> {
            SetupInformationMeta setupInformationMeta = new SetupInformationMeta(onDone);
            deIfmoClient.get(activity, "servlet/distributedCDE?Rule=editPersonProfile", null, new ResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, String response) {
                    thread.standalone(() -> {
                        UserData userData = new UserDataParser(response).parse();
                        if (userData == null) {
                            log.v(TAG, "loginSetupInformation | deIfmoClient | success | not parsed");
                            setupInformationMeta.onDeIfmoFailed();
                            return;
                        }
                        log.v(TAG, "loginSetupInformation | deIfmoClient | success | parsed");
                        String name = StringUtils.defaultIfBlank(userData.getName(), null);
                        List<String> groups = StringUtils.isNotBlank(userData.getGroup()) ?
                                Arrays.asList(userData.getGroup().split(",\\s|\\s|,")) :
                                null;
                        String avatar = StringUtils.defaultIfBlank(userData.getAvatar(), null);
                        Integer week = userData.getWeek() < 0 ? null : userData.getWeek();
                        setupInformationMeta.onDeIfmoReady(name, groups, avatar, week);
                    }, throwable -> {
                        setupInformationMeta.onDeIfmoFailed();
                    });
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    setupInformationMeta.onDeIfmoFailed();
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {}
            });
            isuRestClient.get(activity, "schedule/week/%apikey%", null, new RestResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr) {
                    thread.standalone(() -> {
                        if (obj == null) {
                            setupInformationMeta.onIsuWeekFailed();
                            return;
                        }
                        IsuWeek isuWeek = new IsuWeek().fromJson(obj);
                        setupInformationMeta.onIsuWeekReady(isuWeek.getWeek());
                    }, throwable -> {
                        setupInformationMeta.onIsuWeekFailed();
                    });
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    setupInformationMeta.onIsuWeekFailed();
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Client.Request request) {}
            });
            if (isuPrivateRestClient.isAuthorized(activity)) {
                isuPrivateRestClient.get(activity, "userdata/%apikey%/%isutoken%", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr) {
                        thread.standalone(() -> {
                            if (obj == null) {
                                setupInformationMeta.onIsuUserFailed();
                                return;
                            }
                            IsuUserData isuUserData = new IsuUserData().fromJson(obj);
                            String surname = StringUtils.defaultIfBlank(isuUserData.getSurname(), null);
                            String nameO = StringUtils.defaultIfBlank(isuUserData.getName(), null);
                            String patronymic = StringUtils.defaultIfBlank(isuUserData.getPatronymic(), null);
                            String name = "";
                            if (surname != null) {
                                name += surname.trim();
                                name = name.trim();
                            }
                            if (nameO != null) {
                                name += " " + nameO;
                                name = name.trim();
                            }
                            if (patronymic != null) {
                                name += " " + patronymic;
                                name = name.trim();
                            }
                            List<String> groups = new ArrayList<>();
                            List<IsuUserDataGroup> isuGroups = CollectionUtils.emptyIfNull(isuUserData.getGroups());
                            Collections.reverse(isuGroups);
                            for (IsuUserDataGroup group : isuGroups) {
                                if (StringUtils.isBlank(group.getGroup())) {
                                    continue;
                                }
                                groups.add(group.getGroup());
                            }
                            if (groups.isEmpty()) {
                                groups = null;
                            }
                            String avatar = null;
                            if (isuUserData.getAvatar() != null && StringUtils.isNotBlank(isuUserData.getAvatar().getUrl())) {
                                avatar = isuUserData.getAvatar().getUrl();
                            }
                            setupInformationMeta.onIsuUserReady(name, groups, avatar);
                        }, throwable -> {
                            setupInformationMeta.onIsuUserFailed();
                        });
                    }
                    @Override
                    public void onFailure(int code, Client.Headers headers, int state) {
                        setupInformationMeta.onIsuUserFailed();
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {}
                });
            } else {
                setupInformationMeta.onIsuUserFailed();
            }
        });
    }

    private void logout(String login) {
        log.v(TAG, "logout | login=", login);
        staticUtil.lockOrientation(activity, true);
        account.logout(activity, login, new Account.LogoutHandler() {
            @Override
            public void onSuccess() {
                log.v(TAG, "logout | onSuccess");
                notificationMessage.snackBar(activity, activity.getString(R.string.logged_out));
                show();
                staticUtil.lockOrientation(activity, false);
            }
            @Override
            public void onFailure(String text) {
                log.v(TAG, "logout | onFailure | text=", text);
                notificationMessage.snackBar(activity, text);
                show();
                staticUtil.lockOrientation(activity, false);
            }
            @Override
            public void onProgress(String text) {
                log.v(TAG, "logout | onProgress | text=", text);
                activity.draw(R.layout.state_auth);
                View interrupt_auth_container = activity.findViewById(R.id.interrupt_auth_container);
                if (interrupt_auth_container != null) {
                    interrupt_auth_container.setVisibility(View.GONE);
                }
                TextView loading_message = activity.findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(text);
                }
            }
            @Override
            public void onNewRequest(Client.Request request) {
                requestHandle = request;
            }
        });
    }

    private void displayRemoteMessage() {
        thread.run(() -> firebaseConfigProvider.getMessage(FirebaseConfigProvider.MESSAGE_LOGIN, value -> {
            thread.run(() -> {
                if (value == null) {
                    return;
                }
                int type = value.getType();
                String message = value.getMessage();
                if (StringUtils.isBlank(message)) {
                    return;
                }
                String hash = textUtils.crypt(message);
                if (hash != null && hash.equals(storage.get(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#login", ""))) {
                    return;
                }
                thread.runOnUI(() -> {
                    ViewGroup messageView = activity.findViewById(R.id.message_login);
                    View layout = Message.getRemoteMessage(activity, type, message, (context, view) -> {
                        if (hash == null) {
                            return;
                        }
                        thread.run(() -> {
                            if (!storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#login", hash)) {
                                return;
                            }
                            thread.runOnUI(() -> {
                                if (messageView != null && view != null) {
                                    messageView.removeView(view);
                                }
                            });
                            notificationMessage.snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> thread.run(() -> {
                                if (!storage.delete(activity, Storage.PERMANENT, Storage.GLOBAL, "firebase#remote_message#login")) {
                                    return;
                                }
                                thread.runOnUI(() -> {
                                    if (messageView != null && view != null) {
                                        messageView.addView(view);
                                    }
                                });
                            }));
                        });
                    });
                    if (layout != null && messageView != null) {
                        messageView.removeAllViews();
                        messageView.addView(layout);
                    }
                });
            }, throwable -> {});
        }));
    }

    private class SetupInformationMeta {

        private Runnable onDone;
        private boolean isDeIfmoDone;
        private boolean isIsuUserDone;
        private boolean isIsuWeekDone;
        private String deIfmoName;
        private List<String> deIfmoGroups;
        private String deIfmoAvatar;
        private Integer deIfmoWeek;
        private String isuName;
        private List<String> isuGroups;
        private String isuAvatar;
        private Integer isuWeek;

        SetupInformationMeta(Runnable onDone) {
            this.onDone = onDone;
            this.isDeIfmoDone = false;
            this.isIsuUserDone = false;
            this.isIsuWeekDone = false;
        }

        void onDeIfmoReady(String name, List<String> groups, String avatar, Integer week) {
            isDeIfmoDone = true;
            deIfmoName = name;
            deIfmoGroups = groups;
            deIfmoAvatar = avatar;
            deIfmoWeek = week;
            doneIfReady();
        }

        void onDeIfmoFailed() {
            isDeIfmoDone = true;
            doneIfReady();
        }

        void onIsuUserReady(String name, List<String> groups, String avatar) {
            isIsuUserDone = true;
            isuName = name;
            isuGroups = groups;
            isuAvatar = avatar;
            doneIfReady();
        }

        void onIsuUserFailed() {
            isIsuUserDone = true;
            doneIfReady();
        }

        void onIsuWeekReady(Integer week) {
            isIsuWeekDone = true;
            isuWeek = week;
            doneIfReady();
        }

        void onIsuWeekFailed() {
            isIsuWeekDone = true;
            doneIfReady();
        }

        private void doneIfReady() {
            if (!isDeIfmoDone || !isIsuUserDone || !isIsuWeekDone) {
                return;
            }

            String groupOverride = storagePref.get(activity, "pref_group_force_override", "");
            List<String> groupsOverride = StringUtils.isNotBlank(groupOverride) ?
                    Arrays.asList(groupOverride.split(",\\s|\\s|,")) :
                    null;
            String name = StringUtils.nvlt(isuName, deIfmoName);
            List<String> groups = StringUtils.nvlt(groupsOverride, isuGroups, deIfmoGroups);
            String avatar = StringUtils.nvlt(isuAvatar, deIfmoAvatar);
            Integer week = StringUtils.nvlt(isuWeek, deIfmoWeek);

            if (groups == null) {
                groups = new ArrayList<>();
            }
            String groupCurrent = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group");
            boolean gFound = false;
            for (String g1 : groups) {
                if (Objects.equals(g1, groupCurrent)) {
                    gFound = true;
                    break;
                }
            }
            if (!gFound) {
                groupCurrent = groups.size() > 0 ? groups.get(0) : "";
            }

            String weekStr = "";
            try {
                if (week != null) {
                    weekStr = new UserWeek(week, time.get().getTimeInMillis()).toJsonString();
                }
            } catch (Exception ignore) {}

            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#name", name);
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#group", groupCurrent);
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#groups", TextUtils.join(", ", groups));
            storage.put(activity, Storage.PERMANENT, Storage.USER, "user#avatar", avatar);
            storage.put(activity, Storage.PERMANENT, Storage.GLOBAL, "user#week", weekStr);
            firebaseAnalyticsProvider.setUserProperties(activity, groupCurrent);

            onDone.run();
        }
    }
}
