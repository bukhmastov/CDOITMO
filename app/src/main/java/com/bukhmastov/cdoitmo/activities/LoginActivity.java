package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    public static final int SIGNAL_LOGIN = 0;
    public static final int SIGNAL_RECONNECT = 1;
    public static final int SIGNAL_GO_OFFLINE = 2;
    public static final int SIGNAL_CHANGE_ACCOUNT = 3;
    public static final int SIGNAL_LOGOUT = 4;
    public static final int SIGNAL_CREDENTIALS_REQUIRED = 5;
    public static final int SIGNAL_CREDENTIALS_FAILED = 6;
    private RequestHandle loginRequestHandle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_login));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("  " + getString(R.string.title_activity_login));
            actionBar.setLogo(obtainStyledAttributes(new int[] { R.attr.ic_security }).getDrawable(0));
        }
        route(getIntent().getIntExtra("state", SIGNAL_LOGIN));
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
                    startActivity(new Intent(getBaseContext(), AboutActivity.class));
                    return false;
                }
            });
        }
        return true;
    }

    private void route(int signal){
        Log.i(TAG, "signal = " + signal);
        switch (signal) {
            case SIGNAL_LOGIN:
                Static.OFFLINE_MODE = false;
                show();
                break;
            case SIGNAL_RECONNECT:
                Static.OFFLINE_MODE = false;
                Static.authorized = false;
                show();
                break;
            case SIGNAL_GO_OFFLINE:
                Static.OFFLINE_MODE = true;
                authorized(false);
                break;
            case SIGNAL_CHANGE_ACCOUNT:
                Static.OFFLINE_MODE = false;
                Static.logoutCurrent(this);
                Static.authorized = false;
                show();
                break;
            case SIGNAL_LOGOUT:
                Static.OFFLINE_MODE = false;
                String current_login = Storage.file.general.get(this, "users#current_login");
                if (!current_login.isEmpty()) {
                    logout(current_login);
                } else {
                    show();
                }
                break;
            case SIGNAL_CREDENTIALS_REQUIRED:
                Static.OFFLINE_MODE = false;
                Storage.file.perm.delete(this, "user#jsessionid");
                Static.logoutCurrent(this);
                Static.authorized = false;
                snackBar(getString(R.string.required_login_password));
                show();
                break;
            case SIGNAL_CREDENTIALS_FAILED:
                Static.OFFLINE_MODE = false;
                Storage.file.perm.delete(this, "user#jsessionid");
                Storage.file.perm.delete(this, "user#password");
                Static.logoutCurrent(this);
                Static.authorized = false;
                snackBar(getString(R.string.invalid_login_password));
                show();
                break;
            default:
                Static.OFFLINE_MODE = false;
                Log.wtf(TAG, "unsupported signal: signal=" + signal);
                show();
                break;
        }
    }

    private void show(){
        Log.v(TAG, "show");
        try {
            String current_login = Storage.file.general.get(this, "users#current_login");
            if (!current_login.isEmpty()) {
                String login = Storage.file.perm.get(this, "user#login");
                String password = Storage.file.perm.get(this, "user#password");
                String role = Storage.file.perm.get(this, "user#role");
                auth(login, password, role, false);
            } else {
                LinearLayout login_tiles_container = new LinearLayout(this);
                login_tiles_container.setOrientation(LinearLayout.VERTICAL);
                login_tiles_container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                FrameLayout layout_login_new_user_tile = (FrameLayout) inflate(R.layout.layout_login_new_user_tile);
                final EditText input_login = (EditText) layout_login_new_user_tile.findViewById(R.id.input_login);
                final EditText input_password = (EditText) layout_login_new_user_tile.findViewById(R.id.input_password);
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
                login_tiles_container.addView(layout_login_new_user_tile);
                JSONArray accounts = LoginActivity.accounts.get(this);
                for (int i = 0; i < accounts.length(); i++) {
                    try {
                        Log.v(TAG, "account in accounts " + accounts.getString(i));
                        Storage.file.general.put(this, "users#current_login", accounts.getString(i));
                        final String login = Storage.file.perm.get(this, "user#login");
                        final String password = Storage.file.perm.get(this, "user#password");
                        final String role = Storage.file.perm.get(this, "user#role");
                        final String name = Storage.file.perm.get(this, "user#name");
                        final String group = Storage.file.perm.get(this, "user#group");
                        Storage.file.general.delete(this, "users#current_login");
                        FrameLayout layout_login_user_tile = (FrameLayout) inflate(R.layout.layout_login_user_tile);
                        ((TextView) layout_login_user_tile.findViewById(R.id.name)).setText(name);
                        ((TextView) layout_login_user_tile.findViewById(R.id.desc)).setText(group);
                        layout_login_user_tile.findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.v(TAG, "user_tile logout clicked");
                                logout(login);
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
            snackBar(getString(R.string.something_went_wrong));
        }
    }
    private void auth(final String login, String password, String role, final boolean newUser){
        Log.v(TAG, "auth | login=" + login + " role=" + role + " newUser=" + (newUser ? "true" : "false"));
        if (!login.isEmpty() && !password.isEmpty()) {
            if (Objects.equals(login, "general")) {
                Log.w(TAG, "auth | got login=general that does not supported");
                snackBar(getString(R.string.wrong_login_general));
                return;
            }
            Storage.file.general.put(this, "users#current_login", login);
            Storage.file.perm.put(this, "user#login", login);
            Storage.file.perm.put(this, "user#password", password);
            Storage.file.perm.put(this, "user#role", role);
            DeIfmoClient.check(this, new DeIfmoClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    Log.v(TAG, "auth | check | success");
                    authorized(newUser);
                }
                @Override
                public void onProgress(int state) {
                    Log.v(TAG, "auth | check | progress " + state);
                    draw(R.layout.state_auth);
                    Button interrupt_auth = (Button) findViewById(R.id.interrupt_auth);
                    if (interrupt_auth != null) {
                        interrupt_auth.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.v(TAG, "auth | auth interrupted, going offline");
                                if (loginRequestHandle != null) {
                                    loginRequestHandle.cancel(true);
                                    loginRequestHandle = null;
                                }
                                route(SIGNAL_GO_OFFLINE);
                            }
                        });
                    }
                    TextView loading_message = (TextView) findViewById(R.id.loading_message);
                    if (loading_message != null) {
                        switch (state) {
                            case DeIfmoClient.STATE_CHECKING: loading_message.setText(R.string.auth_check); break;
                            case DeIfmoClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                            case DeIfmoClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                        }
                    }
                }
                @Override
                public void onFailure(int state) {
                    Log.v(TAG, "auth | check | failure " + state);
                    switch (state) {
                        case DeIfmoClient.FAILED_OFFLINE: route(SIGNAL_GO_OFFLINE); return;
                        case DeIfmoClient.FAILED_TRY_AGAIN:
                        case DeIfmoClient.FAILED_AUTH_TRY_AGAIN: snackBar(getString(R.string.auth_failed)); break;
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: snackBar(getString(R.string.required_login_password)); break;
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: snackBar(getString(R.string.invalid_login_password)); break;
                    }
                    logoutDone(login, false);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    loginRequestHandle = requestHandle;
                }
            });
        } else {
            Log.v(TAG, "auth | empty fields");
            snackBar(getString(R.string.fill_fields));
        }
    }
    private void authorized(boolean newUser){
        Log.v(TAG, "authorized | newUser=" + (newUser ? "true" : "false"));
        String current_login = Storage.file.general.get(this, "users#current_login");
        if (!current_login.isEmpty()) {
            accounts.push(this, current_login);
            Static.authorized = true;
            Static.updateWeek(this);
            Static.protocolTracker = new ProtocolTracker(this);
            if (newUser) Static.protocolChangesTrackSetup(this, 0);
            finish();
        } else {
            Log.w(TAG, "authorized | current_login is empty");
            snackBar(getString(R.string.something_went_wrong));
            show();
        }
    }
    private void logout(final String login){
        Log.v(TAG, "logout | login=" + login);
        Storage.file.general.put(this, "users#current_login", login);
        DeIfmoClient.get(this, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new DeIfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                Log.v(TAG, "logout | success");
                logoutDone(login, true);
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "logout | progress " + state);
                draw(R.layout.state_auth);
                Button interrupt_auth = (Button) findViewById(R.id.interrupt_auth);
                if (interrupt_auth != null) {
                    interrupt_auth.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                }
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(getString(R.string.exiting) + "\n" + Storage.file.perm.get(getBaseContext(), "user#name"));
                }
            }
            @Override
            public void onFailure(int state) {
                Log.v(TAG, "logout | failure " + state);
                logoutDone(login, true);
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {}
        }, false);
    }
    private void logoutDone(String login, boolean showSnackBar){
        Log.v(TAG, "logoutDone | login=" + login);
        accounts.remove(this, login);
        Storage.file.general.put(this, "users#current_login", login);
        Static.logout(this);
        if (showSnackBar) snackBar(getString(R.string.logged_out));
        show();
    }

    private static class accounts {
        private static final String TAG = "LoginActivity.accounts";
        private static void push(Context context, String login){
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
            } catch (Exception e) {
                Static.error(e);
            }
        }
        private static void remove(Context context, String login){
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
            } catch (Exception e) {
                Static.error(e);
            }
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

    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.login_content));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void draw(View view){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.login_content));
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void snackBar(String text){
        View activity_login = findViewById(R.id.activity_login);
        if (activity_login != null) {
            Snackbar snackbar = Snackbar.make(activity_login, text, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
            snackbar.show();
        } else {
            Log.w(TAG, "activity_login is null");
        }
    }

}