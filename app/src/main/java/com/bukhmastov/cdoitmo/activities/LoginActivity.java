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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    public static final int SIGNAL_LOGIN = 0;
    public static final int SIGNAL_CREDENTIALS_REQUIRED = 1;
    public static final int SIGNAL_LOGOUT = 2;
    public static final int SIGNAL_CREDENTIALS_FAILED = 3;
    public static final int SIGNAL_RECONNECT = 4;
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
            case SIGNAL_LOGIN: show(); break;
            case SIGNAL_LOGOUT: logOut(); break;
            case SIGNAL_RECONNECT:
                Static.authorized = false;
                show();
                break;
            case SIGNAL_CREDENTIALS_REQUIRED:
                Storage.delete(getBaseContext(), "session_cookie");
                Storage.delete(getBaseContext(), "password");
                snackBar(getString(R.string.required_login_password));
                show();
                break;
            case SIGNAL_CREDENTIALS_FAILED:
                Storage.delete(getBaseContext(), "session_cookie");
                Storage.delete(getBaseContext(), "password");
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
        String login = Storage.get(getBaseContext(), "login");
        String password = Storage.get(getBaseContext(), "password");
        if (Objects.equals(login, "") || Objects.equals(password, "")) {
            draw(R.layout.layout_login_form);
            final EditText input_login = (EditText) findViewById(R.id.input_login);
            final EditText input_password = (EditText) findViewById(R.id.input_password);
            Button btn_login = (Button) findViewById(R.id.btn_login);
            if (btn_login != null) {
                btn_login.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String login = "";
                        String password = "";
                        if (input_login != null) login = input_login.getText().toString();
                        if (input_password != null) password = input_password.getText().toString();
                        if (!(Objects.equals(login, "") || Objects.equals(password, ""))) {
                            Storage.put(getBaseContext(), "login", login);
                            Storage.put(getBaseContext(), "password", password);
                            check();
                        } else {
                            snackBar(getString(R.string.fill_fields));
                        }
                    }
                });
            }
            if (input_login != null) input_login.setText(login);
            if (input_password != null) input_password.setText(password);
        } else {
            check();
        }
    }
    private void check(){
        DeIfmoClient.check(this, new DeIfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                done();
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
                        done();
                        return;
                    case DeIfmoClient.FAILED_TRY_AGAIN:
                    case DeIfmoClient.FAILED_AUTH_TRY_AGAIN: snackBar(getString(R.string.auth_failed)); break;
                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED: snackBar(getString(R.string.required_login_password)); break;
                    case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED: snackBar(getString(R.string.invalid_login_password)); break;
                }
                Storage.delete(getBaseContext(), "session_cookie");
                Storage.delete(getBaseContext(), "password");
                show();
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                authRequestHandle = requestHandle;
            }
        });
    }
    private void done(){
        Static.authorized = true;
        Static.updateWeek(getBaseContext());
        Static.protocolTracker = new ProtocolTracker(getBaseContext());
        finish();
    }
    private void logOut(){
        DeIfmoClient.get(this, "servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new DeIfmoClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                logOutDone();
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_auth);
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                if (loading_message != null) {
                    loading_message.setText(R.string.exiting);
                }
            }
            @Override
            public void onFailure(int state) {
                logOutDone();
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {
                authRequestHandle = requestHandle;
            }
        });
    }
    private void logOutDone(){
        Static.logout(getBaseContext());
        snackBar(getString(R.string.logged_out));
        show();
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