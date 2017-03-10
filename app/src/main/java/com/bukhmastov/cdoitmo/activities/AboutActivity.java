package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

public class AboutActivity extends AppCompatActivity {

    private int counterToReport = 0;
    private int tapsToReport = 7;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false)) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_about));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        TextView app_info_version = (TextView) findViewById(R.id.app_info_version);
        if (app_info_version != null) app_info_version.setText("v" + Static.versionName + " (" + Static.versionCode + ")");
        View app_info_icon = findViewById(R.id.app_info_icon);
        if (app_info_icon != null) {
            app_info_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    counterToReport++;
                    if (counterToReport > 2 && counterToReport < tapsToReport) {
                        if (toast != null) toast.cancel();
                        toast = Toast.makeText(getBaseContext(), "Для отправки отчета нажмите еще " + (tapsToReport - counterToReport) + " раз(а)", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    if (counterToReport == tapsToReport) {
                        counterToReport = 0;
                        if (!Static.errorTracker.send(getBaseContext())) {
                            if (toast != null) toast.cancel();
                            toast = Toast.makeText(getBaseContext(), "Ошибок не найдено", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

}
