package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.fragments.AboutFragment;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class LoginActivity extends ConnectedActivity {

    private static final String TAG = "LoginActivity";
    private final ConnectedActivity activity = this;
    public static final int SIGNAL_LOGIN = 0;
    public static final int SIGNAL_RECONNECT = 1;
    public static final int SIGNAL_GO_OFFLINE = 2;
    public static final int SIGNAL_CHANGE_ACCOUNT = 3;
    public static final int SIGNAL_LOGOUT = 4;
    public static final int SIGNAL_CREDENTIALS_REQUIRED = 5;
    public static final int SIGNAL_CREDENTIALS_FAILED = 6;
    private Client.Request requestHandle = null;
    public static boolean auto_logout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar_login);
        if (toolbar != null) {
            Static.applyToolbarTheme(activity, toolbar);
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
        // IntroducingActivity
        if (Static.isFirstLaunchEver) {
            Static.isFirstLaunchEver = false;
            startActivity(new Intent(activity, IntroducingActivity.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_login, menu);
        MenuItem action_about = menu.findItem(R.id.action_about);
        if (action_about != null) {
            action_about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    final Bundle extras = new Bundle();
                    extras.putBoolean(ACTIVITY_WITH_MENU, false);
                    activity.openActivity(AboutFragment.class, extras);
                    return false;
                }
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "signal = " + signal);
                switch (signal) {
                    case SIGNAL_LOGIN: {
                        Static.OFFLINE_MODE = false;
                        show();
                        break;
                    }
                    case SIGNAL_RECONNECT: {
                        Static.OFFLINE_MODE = false;
                        Static.authorized = false;
                        show();
                        break;
                    }
                    case SIGNAL_GO_OFFLINE: {
                        Static.OFFLINE_MODE = true;
                        authorized(false);
                        break;
                    }
                    case SIGNAL_CHANGE_ACCOUNT: {
                        Static.OFFLINE_MODE = false;
                        Static.logoutCurrent(activity);
                        Static.authorized = false;
                        show();
                        break;
                    }
                    case SIGNAL_LOGOUT: {
                        Static.OFFLINE_MODE = false;
                        String current_login = Storage.file.general.get(activity, "users#current_login");
                        if (!current_login.isEmpty()) {
                            logout(current_login);
                        } else {
                            show();
                        }
                        break;
                    }
                    case SIGNAL_CREDENTIALS_REQUIRED: {
                        Static.OFFLINE_MODE = false;
                        Storage.file.perm.delete(activity, "user#deifmo#cookies");
                        Static.logoutCurrent(activity);
                        Static.authorized = false;
                        Static.snackBar(activity, activity.getString(R.string.required_login_password));
                        show();
                        break;
                    }
                    case SIGNAL_CREDENTIALS_FAILED: {
                        Static.OFFLINE_MODE = false;
                        Storage.file.perm.delete(activity, "user#deifmo#cookies");
                        Storage.file.perm.delete(activity, "user#deifmo#password");
                        Static.logoutCurrent(activity);
                        Static.authorized = false;
                        Static.snackBar(activity, activity.getString(R.string.invalid_login_password));
                        show();
                        break;
                    }
                    default: {
                        Static.OFFLINE_MODE = false;
                        Log.wtf(TAG, "unsupported signal: signal=" + signal);
                        show();
                        break;
                    }
                }
            }
        });
    }

    private void show() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "show");
                try {
                    FirebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.LOGIN_REQUIRED);
                    String current_login = Storage.file.general.get(activity, "users#current_login");
                    if (!current_login.isEmpty()) {
                        String login = Storage.file.perm.get(activity, "user#deifmo#login");
                        String password = Storage.file.perm.get(activity, "user#deifmo#password");
                        String role = Storage.file.perm.get(activity, "user#role");
                        auth(login, password, role, false);
                    } else {
                        LinearLayout login_tiles_container = new LinearLayout(activity);
                        login_tiles_container.setOrientation(LinearLayout.VERTICAL);
                        login_tiles_container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        FrameLayout layout_login_new_user_tile = (FrameLayout) inflate(R.layout.layout_login_new_user_tile);
                        final EditText input_login = layout_login_new_user_tile.findViewById(R.id.input_login);
                        final EditText input_password = layout_login_new_user_tile.findViewById(R.id.input_password);
                        layout_login_new_user_tile.findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.v(TAG, "new_user_tile login clicked");
                                String login = "";
                                String password = "";
                                if (input_login != null) login = input_login.getText().toString();
                                if (input_password != null) password = input_password.getText().toString();
                                auth(login, password, "student", true);
                            }
                        });
                        layout_login_new_user_tile.findViewById(R.id.help).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                FirebaseAnalyticsProvider.logBasicEvent(getBaseContext(), "Help with login clicked");
                                new AlertDialog.Builder(activity)
                                        .setTitle(R.string.auth_help_0)
                                        .setMessage(
                                                activity.getString(R.string.auth_help_1) +
                                                "\n" +
                                                activity.getString(R.string.auth_help_2) +
                                                "\n\n" +
                                                activity.getString(R.string.auth_help_3) +
                                                "\n\n" +
                                                activity.getString(R.string.auth_help_4)
                                        )
                                        .setIcon(R.drawable.ic_help)
                                        .setNegativeButton(R.string.close, null)
                                        .create().show();
                            }
                        });
                        login_tiles_container.addView(layout_login_new_user_tile);
                        final JSONArray accounts = LoginActivity.accounts.get(activity);
                        for (int i = 0; i < accounts.length(); i++) {
                            try {
                                Log.v(TAG, "account in accounts " + accounts.getString(i));
                                Storage.file.general.put(activity, "users#current_login", accounts.getString(i));
                                final String login = Storage.file.perm.get(activity, "user#deifmo#login");
                                final String password = Storage.file.perm.get(activity, "user#deifmo#password");
                                final String role = Storage.file.perm.get(activity, "user#role");
                                final String name = Storage.file.perm.get(activity, "user#name").trim();
                                Storage.file.general.delete(activity, "users#current_login");
                                ViewGroup layout_login_user_tile = (ViewGroup) inflate(R.layout.layout_login_user_tile);
                                View nameView = layout_login_user_tile.findViewById(R.id.name);
                                View descView = layout_login_user_tile.findViewById(R.id.desc);
                                String desc = "";
                                if (!login.isEmpty()) {
                                    desc += login;
                                }
                                switch (role) {
                                    case "student": {
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
                                        Static.removeView(descView);
                                    }
                                }
                                layout_login_user_tile.findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.v(TAG, "user_tile logout clicked");
                                        Static.logoutConfirmation(activity, new Static.SimpleCallback() {
                                            @Override
                                            public void onDone() {
                                                logout(login);
                                            }
                                        });
                                    }
                                });
                                layout_login_user_tile.findViewById(R.id.offline).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Log.v(TAG, "user_tile offline clicked");
                                        Storage.file.general.put(activity, "users#current_login", login);
                                        route(SIGNAL_GO_OFFLINE);
                                    }
                                });
                                layout_login_user_tile.findViewById(R.id.auth).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.v(TAG, "user_tile login clicked");
                                        auth(login, password, role, false);
                                    }
                                });
                                login_tiles_container.addView(layout_login_user_tile);
                            } catch (JSONException e) {
                                Static.error(e);
                            }
                        }
                        draw(login_tiles_container);
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void auth(final String login, final String password, final String role, final boolean newUser) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "auth | login=" + login + " role=" + role + " newUser=" + (newUser ? "true" : "false"));
                if (!login.isEmpty() && !password.isEmpty()) {
                    if (Objects.equals(login, "general")) {
                        Log.w(TAG, "auth | got login=general that does not supported");
                        Static.snackBar(activity, activity.getString(R.string.wrong_login_general));
                        return;
                    }
                    Storage.file.general.put(activity, "users#current_login", login);
                    Storage.file.perm.put(activity, "user#deifmo#login", login);
                    Storage.file.perm.put(activity, "user#deifmo#password", password);
                    Storage.file.perm.put(activity, "user#role", role);
                    DeIfmoClient.check(activity, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Client.Headers headers, String response) {
                            Log.v(TAG, "auth | check | success");
                            authorized(newUser);
                        }
                        @Override
                        public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "auth | check | failure " + state);
                                    switch (state) {
                                        case DeIfmoClient.FAILED_OFFLINE:
                                            route(SIGNAL_GO_OFFLINE);
                                            break;
                                        case DeIfmoClient.FAILED_SERVER_ERROR:
                                            Static.snackBar(activity, activity.getString(R.string.auth_failed) + ". " + DeIfmoClient.getFailureMessage(activity, statusCode));
                                            route(SIGNAL_CHANGE_ACCOUNT);
                                            break;
                                        case DeIfmoClient.FAILED_TRY_AGAIN:
                                        case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                                            Static.snackBar(activity, activity.getString(R.string.auth_failed));
                                            route(SIGNAL_CHANGE_ACCOUNT);
                                            break;
                                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                            Static.snackBar(activity, activity.getString(R.string.required_login_password));
                                            logoutDone(login, false);
                                            route(SIGNAL_CHANGE_ACCOUNT);
                                            break;
                                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                            Static.snackBar(activity, activity.getString(R.string.invalid_login_password));
                                            logoutDone(login, false);
                                            route(SIGNAL_CHANGE_ACCOUNT);
                                            break;
                                    }
                                }
                            });
                        }
                        @Override
                        public void onProgress(final int state) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "auth | check | progress " + state);
                                    draw(R.layout.state_auth);
                                    if (newUser) {
                                        View interrupt_auth_container = findViewById(R.id.interrupt_auth_container);
                                        if (interrupt_auth_container != null) {
                                            interrupt_auth_container.setVisibility(View.GONE);
                                        }
                                    } else {
                                        View interrupt_auth = findViewById(R.id.interrupt_auth);
                                        if (interrupt_auth != null) {
                                            interrupt_auth.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Log.v(TAG, "auth | auth interrupted, going offline");
                                                    if (requestHandle != null) {
                                                        requestHandle.cancel();
                                                    }
                                                    Storage.file.general.put(activity, "users#current_login", login);
                                                    route(SIGNAL_GO_OFFLINE);
                                                }
                                            });
                                        }
                                    }
                                    TextView loading_message = findViewById(R.id.loading_message);
                                    if (loading_message != null) {
                                        switch (state) {
                                            case DeIfmoClient.STATE_CHECKING: loading_message.setText(R.string.auth_check); break;
                                            case DeIfmoClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                                            case DeIfmoClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                                        }
                                    }
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            requestHandle = request;
                        }
                    });
                } else {
                    Log.v(TAG, "auth | empty fields");
                    Static.snackBar(activity, activity.getString(R.string.fill_fields));
                }
            }
        });
    }
    private void authorized(final boolean newUser) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "authorized | newUser=" + (newUser ? "true" : "false"));
                String current_login = Storage.file.general.get(activity, "users#current_login");
                if (!current_login.isEmpty()) {
                    if (newUser) {
                        FirebaseAnalyticsProvider.logBasicEvent(getBaseContext(), "New user authorized");
                    }
                    accounts.push(activity, current_login);
                    Static.authorized = true;
                    if (newUser) Static.protocolChangesTrackSetup(activity, 0);
                    finish();
                } else {
                    Log.w(TAG, "authorized | current_login is empty");
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    show();
                }
            }
        });
    }
    private void logout(final String login) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "logout | login=" + login);
                Storage.file.general.put(activity, "users#current_login", login);
                DeIfmoClient.get(activity, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Log.v(TAG, "logout | success");
                        logoutDone(login, true);
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        Log.v(TAG, "logout | failure " + state);
                        logoutDone(login, true);
                    }
                    @Override
                    public void onProgress(final int state) {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.v(TAG, "logout | progress " + state);
                                draw(R.layout.state_auth);
                                View interrupt_auth_container = findViewById(R.id.interrupt_auth_container);
                                if (interrupt_auth_container != null) {
                                    interrupt_auth_container.setVisibility(View.GONE);
                                }
                                TextView loading_message = findViewById(R.id.loading_message);
                                if (loading_message != null) {
                                    loading_message.setText(activity.getString(R.string.exiting) + "\n" + Storage.file.perm.get(getBaseContext(), "user#name"));
                                }
                            }
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            }
        });
    }
    private void logoutDone(final String login, final boolean showSnackBar) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "logoutDone | login=" + login);
                accounts.remove(activity, login);
                Storage.file.general.put(activity, "users#current_login", login);
                Static.logout(activity);
                if (showSnackBar) Static.snackBar(activity, activity.getString(R.string.logged_out));
                show();
            }
        });
    }
    private static class accounts {
        private static final String TAG = "LoginActivity.accounts";
        private static void push(final Context context, final String login) {
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "push | login=" + login);
                    String list = Storage.file.general.get(context, "users#list");
                    try {
                        JSONArray accounts;
                        if (list.isEmpty()) {
                            accounts = new JSONArray();
                        } else {
                            accounts = new JSONArray(list);
                        }
                        boolean found = false;
                        for (int i = 0; i < accounts.length(); i++) {
                            if (Objects.equals(accounts.getString(i), login)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) accounts.put(login);
                        Storage.file.general.put(context, "users#list", accounts.toString());
                        Bundle bundle;
                        bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, accounts.length());
                        bundle = FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_NEW, found ? "old" : "new", bundle);
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
        private static void remove(final Context context, final String login) {
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "remove | login=" + login);
                    String list = Storage.file.general.get(context, "users#list");
                    try {
                        JSONArray accounts;
                        if (list.isEmpty()) {
                            accounts = new JSONArray();
                        } else {
                            accounts = new JSONArray(list);
                        }
                        for (int i = 0; i < accounts.length(); i++) {
                            if (Objects.equals(accounts.getString(i), login)) {
                                accounts.remove(i);
                                break;
                            }
                        }
                        Storage.file.general.put(context, "users#list", accounts.toString());
                        FirebaseAnalyticsProvider.logEvent(
                                context,
                                FirebaseAnalyticsProvider.Event.LOGOUT,
                                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LOGIN_COUNT, accounts.length())
                        );
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            });
        }
        private static JSONArray get(Context context){
            Log.v(TAG, "get");
            String list = Storage.file.general.get(context, "users#list");
            JSONArray accounts = new JSONArray();
            try {
                if (!list.isEmpty()) accounts = new JSONArray(list);
            } catch (Exception e) {
                Static.error(e);
            }
            return accounts;
        }
    }

    private void displayRemoteMessage() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                FirebaseConfigProvider.getJson(FirebaseConfigProvider.MESSAGE_LOGIN, new FirebaseConfigProvider.ResultJson() {
                    @Override
                    public void onResult(final JSONObject value) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (value == null) return;
                                    final int type = value.getInt("type");
                                    final String message = value.getString("message");
                                    if (message == null || message.trim().isEmpty()) return;
                                    Static.T.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            View layout = Static.getRemoteMessage(activity, type, message);
                                            if (layout != null) {
                                                ViewGroup message_login = activity.findViewById(R.id.message_login);
                                                if (message_login != null) {
                                                    message_login.removeAllViews();
                                                    message_login.addView(layout);
                                                }
                                            }
                                        }
                                    });
                                } catch (Exception ignore) {
                                    // ignore
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void draw(final int layoutId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = findViewById(R.id.login_content);
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private void draw(final View view) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = findViewById(R.id.login_content);
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(view);
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }
    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
