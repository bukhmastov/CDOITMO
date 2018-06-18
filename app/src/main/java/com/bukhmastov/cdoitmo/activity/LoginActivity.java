package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.data.AccountList;
import com.bukhmastov.cdoitmo.data.StorageProxy;
import com.bukhmastov.cdoitmo.data.User;
import com.bukhmastov.cdoitmo.data.UserCredentials;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.view.Message;

public class LoginActivity extends ConnectedActivity {

    private static final String TAG = "LoginActivity";

    public static final int SIGNAL_LOGIN = 0;
    public static final int SIGNAL_RECONNECT = 1;
    public static final int SIGNAL_GO_OFFLINE = 2;
    public static final int SIGNAL_CHANGE_ACCOUNT = 3;
    public static final int SIGNAL_DO_CLEAN_AUTH = 4;
    public static final int SIGNAL_LOGOUT = 5;
    public static final int SIGNAL_CREDENTIALS_REQUIRED = 6;
    public static final int SIGNAL_CREDENTIALS_FAILED = 7;

    private Client.Request requestHandle = null;
    public static boolean auto_logout = false;

    private StorageProxy storage = new Storage.Proxy(activity);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_login);
        // Show introducing activity
        if (App.showIntroducingActivity) {
            App.showIntroducingActivity = false;
            activity.startActivity(new Intent(activity, IntroducingActivity.class));
        }
        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_login);
        if (toolbar != null) {
            Theme.applyToolbarTheme(activity, toolbar);
            setSupportActionBar(toolbar);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(activity.getString(R.string.title_activity_login));
            actionBar.setLogo(obtainStyledAttributes(new int[] { R.attr.ic_toolbar_security }).getDrawable(0));
        }
        displayRemoteMessage();
        if (LoginActivity.auto_logout) {
            LoginActivity.auto_logout = false;
            route(SIGNAL_LOGOUT);
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                route(intent.getIntExtra("state", SIGNAL_LOGIN));
            } else {
                route(SIGNAL_LOGIN);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        toolbar = menu;
        final MenuItem action_about = menu.findItem(R.id.action_about);
        if (action_about != null) {
            action_about.setVisible(true);
            action_about.setOnMenuItemClickListener(item -> {
                final Bundle extras = new Bundle();
                extras.putBoolean(ACTIVITY_WITH_MENU, false);
                activity.openActivity(AboutFragment.class, extras);
                return false;
            });
        }
        return true;
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    @Override
    protected int getRootViewId() {
        return R.id.login_content;
    }

    private void route(final int signal) {
        Thread.run(() -> {
            Log.i(TAG, "route | signal=", signal);
            App.OFFLINE_MODE = false;
            App.UNAUTHORIZED_MODE = false;
            switch (signal) {
                case SIGNAL_LOGIN: {
                    show();
                    break;
                }
                case SIGNAL_RECONNECT: {
                    Account.authorized = false;
                    show();
                    break;
                }
                case SIGNAL_GO_OFFLINE: {
                    App.OFFLINE_MODE = true;
                    show();
                    break;
                }
                case SIGNAL_CHANGE_ACCOUNT: {
                    if (UserCredentials.hasCurrentLogin(storage)) {
                        Account.clearAuthorization(/* permanently? */ false, activity, this::show);
                    } else {
                        show();
                    }
                    break;
                }
                case SIGNAL_DO_CLEAN_AUTH: {
                    UserCredentials.clearCookies(storage);
                    show();
                    break;
                }
                case SIGNAL_LOGOUT: {
                    String currentLogin = UserCredentials.getCurrentLogin(storage);
                    if (!currentLogin.isEmpty()) {
                        logout(currentLogin);
                    } else {
                        show();
                    }
                    break;
                }
                case SIGNAL_CREDENTIALS_REQUIRED: {
                    if (UserCredentials.hasCurrentLogin(storage)) {
                        UserCredentials.clearCookies(storage);
                        Account.clearAuthorization(/* permanently? */ false, activity, () -> {
                            BottomBar.snackBar(activity, activity.getString(R.string.required_login_password));
                            show();
                        });
                    }
                    show();
                    break;
                }
                case SIGNAL_CREDENTIALS_FAILED: {
                    if (UserCredentials.hasCurrentLogin(storage)) {
                        UserCredentials.clearCookies(storage);
                        UserCredentials.clearPassword(storage);
                        Account.clearAuthorization(/* permanently? */ false, activity, () -> {
                            BottomBar.snackBar(activity, activity.getString(R.string.invalid_login_password));
                            show();
                        });
                    }
                    show();
                    break;
                }
                default: {
                    Log.wtf(TAG, "route | unsupported signal: signal=", signal, " | going to use signal=SIGNAL_LOGIN");
                    route(SIGNAL_LOGIN);
                    break;
                }
            }
        });
    }
    private void show() {
        Thread.run(() -> {
            try {
                Log.v(TAG, "show");
                FirebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGIN_REQUIRED);
                UserCredentials credentials = UserCredentials.load(new Storage.Proxy(activity));
                if (credentials != null) {
                    // already logged in
                    login(credentials, false);
                } else {
                    // show login UI
                    final LinearLayout login_tiles_container = new LinearLayout(activity);
                    login_tiles_container.setOrientation(LinearLayout.VERTICAL);
                    login_tiles_container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    // show UI: new user
                    final ViewGroup new_user_tile = (ViewGroup) inflate(R.layout.layout_login_new_user_tile);
                    final EditText input_login = new_user_tile.findViewById(R.id.input_login);
                    final EditText input_password = new_user_tile.findViewById(R.id.input_password);
                    new_user_tile.findViewById(R.id.login).setOnClickListener(v -> {
                        Log.v(TAG, "new_user_tile login clicked");
                        String login = "";
                        String password = "";
                        if (input_login != null) login = input_login.getText().toString();
                        if (input_password != null) password = input_password.getText().toString();
                        login(new UserCredentials(login, password, "student"), true);
                    });
                    new_user_tile.findViewById(R.id.help).setOnClickListener(view -> {
                        FirebaseAnalyticsProvider.logBasicEvent(getBaseContext(), "Help with login clicked");
                        new AlertDialog.Builder(activity)
                                .setIcon(R.drawable.ic_help)
                                .setTitle(R.string.auth_help_0)
                                .setMessage(
                                        activity.getString(R.string.auth_help_1) + "\n" +
                                        activity.getString(R.string.auth_help_2) + "\n\n" +
                                        activity.getString(R.string.auth_help_3) + "\n" +
                                        activity.getString(R.string.auth_help_4) + "\n\n" +
                                        activity.getString(R.string.auth_help_5)
                                )
                                .setNegativeButton(R.string.close, null)
                                .create().show();
                    });
                    login_tiles_container.addView(new_user_tile);
                    // show UI: list of existing accounts
                    for (AccountList.AccountEntry account : new AccountList(storage)) {
                        if (account == null) continue;
                        Log.v(TAG, "show | account in accounts | ", account.creds.getLogin());
                        final ViewGroup user_tile = (ViewGroup) inflate(R.layout.layout_login_user_tile);
                        View nameView = user_tile.findViewById(R.id.name);
                        View descView = user_tile.findViewById(R.id.desc);
                        String desc = "";
                        if (!account.creds.getLogin().isEmpty()) {
                            desc += account.creds.getLogin();
                        }
                        switch (account.creds.getRole()) {
                            case "student": {
                                desc += desc.isEmpty() ? activity.getString(R.string.student) : " (" + activity.getString(R.string.student) + ")";
                                break;
                            }
                            default: {
                                desc += desc.isEmpty() ? account.creds.getRole() : " (" + account.creds.getRole() + ")";
                                break;
                            }
                        }
                        if (!account.name.isEmpty()) {
                            if (nameView != null) {
                                ((TextView) nameView).setText(account.name);
                            }
                            if (descView != null) {
                                ((TextView) descView).setText(desc);
                            }
                        } else {
                            if (nameView != null) {
                                ((TextView) nameView).setText(desc);
                            }
                            if (descView != null) {
                                Static.removeView(descView);
                            }
                        }
                        user_tile.findViewById(R.id.auth).setOnClickListener(v -> {
                            Log.v(TAG, "user_tile login clicked");
                            login(account.creds, false);
                        });
                        user_tile.findViewById(R.id.expand_auth_menu).setOnClickListener(view -> {
                            Log.v(TAG, "user_tile expand_auth_menu clicked");
                            final PopupMenu popup = new PopupMenu(activity, view);
                            final Menu menu = popup.getMenu();
                            popup.getMenuInflater().inflate(R.menu.auth_expanded_menu, menu);
                            popup.setOnMenuItemClickListener(item -> {
                                Log.v(TAG, "auth_expanded_menu | popup.MenuItem clicked | ", item.getTitle().toString());
                                switch (item.getItemId()) {
                                    case R.id.offline: {
                                        UserCredentials.setCurrentLogin(storage, account.creds.getLogin());
                                        route(SIGNAL_GO_OFFLINE);
                                        break;
                                    }
                                    case R.id.clean_auth: {
                                        UserCredentials.setCurrentLogin(storage, account.creds.getLogin());
                                        route(SIGNAL_DO_CLEAN_AUTH);
                                        break;
                                    }
                                    case R.id.logout: {
                                        Account.logoutConfirmation(activity, () -> logout(account.creds.getLogin()));
                                        break;
                                    }
                                    case R.id.change_password: {
                                        Thread.runOnUI(() -> {
                                            try {
                                                final View layout = inflate(R.layout.preference_dialog_input);
                                                final EditText editText = layout.findViewById(R.id.edittext);
                                                final TextView message = layout.findViewById(R.id.message);
                                                editText.setHint(R.string.new_password);
                                                message.setText(activity.getString(R.string.change_password_message).replace("%login%", account.creds.getLogin()));
                                                new AlertDialog.Builder(activity)
                                                        .setTitle(R.string.change_password_title)
                                                        .setView(layout)
                                                        .setPositiveButton(R.string.accept, (dialog, which) -> {
                                                            try {
                                                                final String value = editText.getText().toString().trim();
                                                                if (!value.isEmpty()) {
                                                                    Thread.run(() -> {
                                                                        UserCredentials.changePasswordByLogin(storage, account.creds.getLogin(), value);
                                                                        BottomBar.snackBar(activity, activity.getString(R.string.password_changed));
                                                                    });
                                                                }
                                                            } catch (Exception e) {
                                                                Log.exception(e);
                                                            }
                                                        })
                                                        .setNegativeButton(R.string.cancel, null)
                                                        .create().show();
                                            } catch (Exception e) {
                                                Log.exception(e);
                                            }
                                        });
                                        break;
                                    }
                                }
                                return false;
                            });
                            popup.show();
                        });
                        login_tiles_container.addView(user_tile);
                    }
                    // show UI: anonymous login
                    final ViewGroup anonymous_user_tile = (ViewGroup) inflate(R.layout.layout_login_anonymous_user_tile);
                    final EditText input_group = anonymous_user_tile.findViewById(R.id.input_group);
                    anonymous_user_tile.findViewById(R.id.login).setOnClickListener(view -> {
                        Log.v(TAG, "anonymous_user_tile login clicked");

                        String group = "";
                        if (input_group != null) {
                            group = com.bukhmastov.cdoitmo.util.TextUtils.prettifyGroupNumber(input_group.getText().toString());
                        }

                        UserCredentials.setCurrentLogin(storage, UserCredentials.LOGIN_UNAUTHORIZED);
                        new User(activity.getString(R.string.anonymous), "", group, null).store(storage);
                        UserCredentials.resetCurrentLogin(storage);
                        login(UserCredentials.UNAUTHORIZED, false);
                    });
                    anonymous_user_tile.findViewById(R.id.expand_auth_menu).setOnClickListener(view -> {
                        Log.v(TAG, "anonymous_user_tile expand_auth_menu clicked");
                        final PopupMenu popup = new PopupMenu(activity, view);
                        final Menu menu = popup.getMenu();
                        popup.getMenuInflater().inflate(R.menu.auth_anonymous_expanded_menu, menu);
                        popup.setOnMenuItemClickListener(item -> {
                            Log.v(TAG, "auth_expanded_menu | popup.MenuItem clicked | ", item.getTitle().toString());
                            if (item.getItemId() == R.id.offline) {
                                UserCredentials.setCurrentLogin(storage, UserCredentials.LOGIN_UNAUTHORIZED);
                                route(SIGNAL_GO_OFFLINE);
                            }
                            return false;
                        });
                        popup.show();
                    });
                    anonymous_user_tile.findViewById(R.id.info).setOnClickListener(view -> {
                        FirebaseAnalyticsProvider.logBasicEvent(getBaseContext(), "Help with anonymous login clicked");
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
                    login_tiles_container.addView(anonymous_user_tile);
                    Storage.file.general.perm.put(activity, "users#current_login", UserCredentials.LOGIN_UNAUTHORIZED);
                    input_group.setText(Storage.file.perm.get(activity, "user#groups", ""));
                    Storage.file.general.perm.delete(activity, "users#current_login");
                    // draw UI
                    Thread.runOnUI(() -> {
                        try {
                            draw(login_tiles_container);
                        } catch (Exception e) {
                            Log.exception(e);
                            BottomBar.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    });
                }
            } catch (Exception e) {
                Log.exception(e);
                BottomBar.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    private void login(final UserCredentials creds, final boolean isNewUser) {
        Log.v(TAG, "login | login=", creds.getLogin(), " | role=", creds.getRole(), " | isNewUser=", isNewUser);
        Static.lockOrientation(activity, true);
        Account.login(activity, creds, isNewUser, new Account.LoginHandler() {
            @Override
            public void onSuccess() {
                Log.v(TAG, "login | onSuccess");
                finish();
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onOffline() {
                Log.v(TAG, "login | onOffline");
                finish();
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onInterrupted() {
                Log.v(TAG, "login | onInterrupted");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "login interrupted");
                UserCredentials.setCurrentLogin(storage, creds.getLogin());
                route(SIGNAL_GO_OFFLINE);
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onFailure(String text) {
                Log.v(TAG, "login | onFailure | text=", text);
                BottomBar.snackBar(activity, text);
                show();
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onProgress(String text) {
                Log.v(TAG, "login | onProgress | text=", text);
                draw(R.layout.state_auth);
                if (isNewUser) {
                    View interrupt_auth_container = findViewById(R.id.interrupt_auth_container);
                    if (interrupt_auth_container != null) {
                        interrupt_auth_container.setVisibility(View.GONE);
                    }
                } else {
                    View interrupt_auth = findViewById(R.id.interrupt_auth);
                    if (interrupt_auth != null) {
                        interrupt_auth.setOnClickListener(v -> {
                            Log.v(TAG, "login | onProgress | login interrupt clicked");
                            if (requestHandle != null && requestHandle.cancel()) {
                                Log.v(TAG, "login | onProgress | login interrupted");
                            }
                        });
                    }
                }
                TextView loading_message = findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(text);
                }
            }
            @Override
            public void onNewRequest(Client.Request request) {
                Log.v(TAG, "login | onNewRequest");
                requestHandle = request;
            }
        });
    }
    private void logout(final String login) {
        Log.v(TAG, "logout | login=", login);
        Static.lockOrientation(activity, true);
        Account.logout(activity, login, new Account.LogoutHandler() {
            @Override
            public void onSuccess() {
                Log.v(TAG, "logout | onSuccess");
                BottomBar.snackBar(activity, activity.getString(R.string.logged_out));
                show();
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onFailure(String text) {
                Log.v(TAG, "logout | onFailure | text=", text);
                BottomBar.snackBar(activity, text);
                show();
                Static.lockOrientation(activity, false);
            }
            @Override
            public void onProgress(String text) {
                Log.v(TAG, "logout | onProgress | text=", text);
                draw(R.layout.state_auth);
                View interrupt_auth_container = findViewById(R.id.interrupt_auth_container);
                if (interrupt_auth_container != null) {
                    interrupt_auth_container.setVisibility(View.GONE);
                }
                TextView loading_message = findViewById(R.id.loading_message);
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
        Thread.run(() -> FirebaseConfigProvider.getJson(FirebaseConfigProvider.MESSAGE_LOGIN, value -> Thread.run(() -> {
            try {
                if (value == null) return;
                final int type = value.getInt("type");
                final String message = value.getString("message");
                if (message == null || message.trim().isEmpty()) return;
                final String hash = com.bukhmastov.cdoitmo.util.TextUtils.crypt(message);
                if (hash != null && hash.equals(Storage.file.general.perm.get(activity, "firebase#remote_message#login", ""))) {
                    return;
                }
                Thread.runOnUI(() -> {
                    final ViewGroup message_login = activity.findViewById(R.id.message_login);
                    final View layout = Message.getRemoteMessage(activity, type, message, (context, view) -> {
                        if (hash != null) {
                            Thread.run(() -> {
                                if (Storage.file.general.perm.put(activity, "firebase#remote_message#login", hash)) {
                                    Thread.runOnUI(() -> {
                                        if (message_login != null && view != null) {
                                            message_login.removeView(view);
                                        }
                                    });
                                    BottomBar.snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> Thread.run(() -> {
                                        if (Storage.file.general.perm.delete(activity, "firebase#remote_message#login")) {
                                            Thread.runOnUI(() -> {
                                                if (message_login != null && view != null) {
                                                    message_login.addView(view);
                                                }
                                            });
                                        }
                                    }));
                                }
                            });
                        }
                    });
                    if (layout != null && message_login != null) {
                        message_login.removeAllViews();
                        message_login.addView(layout);
                    }
                });
            } catch (Exception ignore) {
                // ignore
            }
        })));
    }
}
