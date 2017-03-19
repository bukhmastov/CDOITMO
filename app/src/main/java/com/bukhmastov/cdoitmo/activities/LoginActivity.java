package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
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
    public static final int SIGNAL_CHANGE_ACCOUNT = 2;
    public static final int SIGNAL_LOGOUT = 3;
    public static final int SIGNAL_CREDENTIALS_REQUIRED = 4;
    public static final int SIGNAL_CREDENTIALS_FAILED = 5;
    private RequestHandle authRequestHandle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_login));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle("  " + getString(R.string.title_activity_login));
            actionBar.setLogo(obtainStyledAttributes(new int[] { R.attr.ic_security }).getDrawable(0));
        }
        Static.OFFLINE_MODE = false;
        switch (getIntent().getIntExtra("state", SIGNAL_LOGIN)) {
            case SIGNAL_LOGIN:
                show();
                break;
            case SIGNAL_RECONNECT:
                Static.authorized = false;
                show();
                break;
            case SIGNAL_CHANGE_ACCOUNT:
                Static.logoutCurrent(this);
                Static.authorized = false;
                show();
                break;
            case SIGNAL_LOGOUT:
                String current_login = Storage.file.general.get(this, "users#current_login");
                if (!current_login.isEmpty()) {
                    logout(current_login);
                } else {
                    show();
                }
                break;
            case SIGNAL_CREDENTIALS_REQUIRED:
                Storage.file.perm.delete(this, "user#jsessionid");
                Static.logoutCurrent(this);
                Static.authorized = false;
                snackBar(getString(R.string.required_login_password));
                show();
                break;
            case SIGNAL_CREDENTIALS_FAILED:
                Storage.file.perm.delete(this, "user#jsessionid");
                Storage.file.perm.delete(this, "user#password");
                Static.logoutCurrent(this);
                Static.authorized = false;
                snackBar(getString(R.string.invalid_login_password));
                show();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authRequestHandle != null) authRequestHandle.cancel(true);
    }

    private void show(){
        try {
            String current_login = Storage.file.general.get(this, "users#current_login");
            if (!current_login.isEmpty()) {
                String login = Storage.file.perm.get(this, "user#login");
                String password = Storage.file.perm.get(this, "user#password");
                String role = Storage.file.perm.get(this, "user#role");
                auth(login, password, role);
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
                        String login = "";
                        String password = "";
                        if (input_login != null) login = input_login.getText().toString();
                        if (input_password != null) password = input_password.getText().toString();
                        auth(login, password, "student");
                    }
                });
                login_tiles_container.addView(layout_login_new_user_tile);
                JSONArray accounts = LoginActivity.accounts.get(this);
                for (int i = 0; i < accounts.length(); i++) {
                    try {
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
                                logout(login);
                            }
                        });
                        layout_login_user_tile.findViewById(R.id.auth).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                auth(login, password, role);
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
    private void auth(final String login, String password, String role){
        if (!login.isEmpty() && !password.isEmpty()) {
            Storage.file.general.put(this, "users#current_login", login);
            Storage.file.perm.put(this, "user#login", login);
            Storage.file.perm.put(this, "user#password", password);
            Storage.file.perm.put(this, "user#role", role);
            DeIfmoClient.check(this, new DeIfmoClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    authorized();
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_auth);
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
                    switch (state) {
                        case DeIfmoClient.FAILED_OFFLINE:
                            Static.OFFLINE_MODE = true;
                            authorized();
                            return;
                        case DeIfmoClient.FAILED_TRY_AGAIN:
                        case DeIfmoClient.FAILED_AUTH_TRY_AGAIN: snackBar(getString(R.string.auth_failed)); break;
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: snackBar(getString(R.string.required_login_password)); break;
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: snackBar(getString(R.string.invalid_login_password)); break;
                    }
                    logoutDone(login, false);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    authRequestHandle = requestHandle;
                }
            });
        } else {
            snackBar(getString(R.string.fill_fields));
        }
    }
    private void authorized(){
        String current_login = Storage.file.general.get(this, "users#current_login");
        if (!current_login.isEmpty()) {
            accounts.push(this, current_login);
            Static.authorized = true;
            Static.updateWeek(this);
            Static.protocolTracker = new ProtocolTracker(this);
            finish();
        } else {
            snackBar(getString(R.string.something_went_wrong));
            show();
        }
    }
    private void logout(final String login){
        Storage.file.general.put(this, "users#current_login", login);
        DeIfmoClient.get(this, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new DeIfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                logoutDone(login, true);
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_auth);
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(getString(R.string.exiting) + "\n" + Storage.file.perm.get(getBaseContext(), "user#name"));
                }
            }
            @Override
            public void onFailure(int state) {
                logoutDone(login, true);
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                authRequestHandle = requestHandle;
            }
        }, false);
    }
    private void logoutDone(String login, boolean showSnackBar){
        accounts.remove(this, login);
        Storage.file.general.put(this, "users#current_login", login);
        Static.logout(this);
        if (showSnackBar) snackBar(getString(R.string.logged_out));
        show();
    }

    private static class accounts {
        private static void push(Context context, String login){
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