package com.bukhmastov.cdoitmo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private SharedPreferences sharedPreferences;
    private Button btn_login;
    private EditText input_login, input_password;
    public static int state = -1;
    static final int SIGNAL_CREDENTIALS_REQUIRED = 0;
    static final int SIGNAL_LOGOUT = 1;
    static final int SIGNAL_CREDENTIALS_FAILED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_login));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle("  " + getString(R.string.title_activity_login));
            actionBar.setLogo(R.drawable.ic_security);
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // инициализация http клиента
        DeIfmoRestClient.init(this);
        // инициализация визуальных компонентов
        btn_login = (Button) findViewById(R.id.btn_login);
        input_login = (EditText) findViewById(R.id.input_login);
        input_password = (EditText) findViewById(R.id.input_password);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = input_login.getText().toString();
                String password = input_password.getText().toString();
                if(!(Objects.equals(login, "") || Objects.equals(password, ""))){
                    Log.d(TAG, "Login: " + login + " | Password: " + password);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("login", login);
                    editor.putString("password", password);
                    editor.apply();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                } else {
                    Snackbar.make(findViewById(R.id.activity_login), R.string.fill_fields, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch(state){
            case SIGNAL_LOGOUT: logOut(); break;
            case SIGNAL_CREDENTIALS_FAILED: // неверные учетные данные
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("u_name", "");
                editor.putString("password", "");
                editor.putString("session_cookie", "");
                editor.apply();
                Snackbar.make(findViewById(R.id.activity_login), R.string.invalid_login_password, Snackbar.LENGTH_LONG).show();
                break;
        }
        state = -1;
        if (!(Objects.equals(sharedPreferences.getString("login", ""), "") || Objects.equals(sharedPreferences.getString("password", ""), ""))){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        } else {
            input_login.setText(sharedPreferences.getString("login", ""));
            input_password.setText(sharedPreferences.getString("password", ""));
        }
    }

    private void logOut(){
        DeIfmoRestClient.get("servlet/distributedCDE?Rule=SYSTEM_EXIT", null, new DeIfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {}
            @Override
            public void onProgress(int state) {}
            @Override
            public void onFailure(int state) {}
        });
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("u_name", "");
        editor.putString("login", "");
        editor.putString("password", "");
        editor.putString("session_cookie", "");
        editor.apply();
        Snackbar.make(findViewById(R.id.activity_login), R.string.logged_out, Snackbar.LENGTH_SHORT).show();
    }
}
