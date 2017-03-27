package com.bukhmastov.cdoitmo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Random;

public class AboutActivity extends AppCompatActivity {

    private final Random random = new Random();
    private int counterToReport = 0;
    private int tapsToReport = 7;
    private int counterToPika = 0;
    private int tapsToPika = 5;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
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
                    if (counterToPika >= tapsToPika) {
                        if (random.nextInt(200) % 10 == 0) {
                            startActivity(new Intent(getBaseContext(), PikaActivity.class));
                        }
                    } else {
                        counterToPika++;
                    }
                }
            });
        }
        View toolbar_about = findViewById(R.id.toolbar_about);
        if (toolbar_about != null) {
            toolbar_about.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    counterToReport++;
                    if (counterToReport > 3 && counterToReport < tapsToReport) {
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
