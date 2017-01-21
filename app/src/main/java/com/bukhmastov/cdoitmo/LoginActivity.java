package com.bukhmastov.cdoitmo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.loopj.android.http.RequestHandle;

import java.util.ArrayList;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    static ErrorTracker errorTracker;
    static String versionName;
    static int versionCode;
    private Button btn_login;
    private EditText input_login, input_password;
    public static int state = -1;
    static final int SIGNAL_CREDENTIALS_REQUIRED = 0;
    static final int SIGNAL_LOGOUT = 1;
    static final int SIGNAL_CREDENTIALS_FAILED = 2;
    static final int SIGNAL_RECONNECT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false)) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        errorTracker = new ErrorTracker(this);
        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_login));
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle("  " + getString(R.string.title_activity_login));
            int[] attrs = new int[] { R.attr.ic_security };
            actionBar.setLogo(obtainStyledAttributes(attrs).getDrawable(0));
        }
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
                    Storage.put(getBaseContext(), "login", login);
                    Storage.put(getBaseContext(), "password", password);
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
        try {
            switch (state) {
                case SIGNAL_CREDENTIALS_REQUIRED: break;
                case SIGNAL_LOGOUT: logOut(); break;
                case SIGNAL_CREDENTIALS_FAILED: // неверные учетные данные
                    Storage.clear(getBaseContext());
                    Snackbar.make(findViewById(R.id.activity_login), R.string.invalid_login_password, Snackbar.LENGTH_LONG).show();
                    break;
                case SIGNAL_RECONNECT: break;
                default:
                    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_auto_logout", false) && !(Objects.equals(Storage.get(getBaseContext(), "login"), "") || Objects.equals(Storage.get(getBaseContext(), "password"), ""))) logOut();
                    break;
            }
            state = -1;
            if (!(Objects.equals(Storage.get(getBaseContext(), "login"), "") || Objects.equals(Storage.get(getBaseContext(), "password"), ""))) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            } else {
                input_login.setText(Storage.get(getBaseContext(), "login"));
                input_password.setText(Storage.get(getBaseContext(), "password"));
            }
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
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
            @Override
            public void onNewHandle(RequestHandle requestHandle) {}
        });
        Storage.clear(getBaseContext());
        Cache.clear(getBaseContext());
        new ProtocolTracker(this).stop();
        try {
            Snackbar.make(findViewById(R.id.activity_login), R.string.logged_out, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
        }
    }
}

class ErrorTracker {
    private ArrayList<Throwable> errorList = new ArrayList<>();
    private Context context;
    ErrorTracker(Context context){
        this.context = context;
    }
    void add(Throwable throwable){
        throwable.printStackTrace();
        errorList.add(throwable);
    }
    int count(){
        return errorList.size();
    }
    boolean send(){
        if(errorList.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--------Device--------").append("\n");
            stringBuilder.append("DEVICE: ").append(Build.DEVICE).append("\n");
            stringBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
            stringBuilder.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
            stringBuilder.append("DISPLAY: ").append(Build.DISPLAY).append("\n");
            stringBuilder.append("SDK_INT: ").append(Build.VERSION.SDK_INT).append("\n");
            stringBuilder.append("--------Application--------").append("\n");
            stringBuilder.append(LoginActivity.versionName).append(" (").append(LoginActivity.versionCode).append(")").append("\n");
            for (Throwable throwable : errorList) {
                stringBuilder.append("--------Stack trace--------").append("\n");
                stringBuilder.append(throwable.getMessage()).append("\n");
                StackTraceElement[] stackTrace = throwable.getStackTrace();
                for (StackTraceElement element : stackTrace)
                    stringBuilder.append("at ").append(element.toString()).append("\n");
            }
            errorList.clear();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"bukhmastov-alex@ya.ru"});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "CDO ITMO - report");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, stringBuilder.toString());
            try {
                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.error_choose_program)));
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }
}