package com.bukhmastov.cdoitmo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import java.io.File;
import java.io.FileWriter;

public class LogActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "LogActivity";
    private Log.ExtraLog extraLog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_log);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_log));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        display();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public void onRefresh() {
        display();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            case R.id.action_log_download:
                if (extraLog != null) {
                    File logFile = getLogFile(extraLog.log);
                    if (logFile != null) {
                        Uri tempUri = FileProvider.getUriForFile(this, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(tempUri, getContentResolver().getType(tempUri));
                        intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                        startActivity(Intent.createChooser(intent, getString(R.string.share) + "..."));
                    }
                }
                return true;
            case R.id.action_log_send_mail:
                if (extraLog != null) {
                    File logFile = getLogFile(extraLog.log);
                    if (logFile != null) {
                        Uri tempUri = FileProvider.getUriForFile(this, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bukhmastov-alex@ya.ru"});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "CDOITMO - log report");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(tempUri, getContentResolver().getType(tempUri));
                        intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                        startActivity(Intent.createChooser(intent, getString(R.string.send_mail) + "..."));
                    }
                }
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    private File getLogFile(String data){
        try {
            File temp = new File(new File(getCacheDir(), "shared"), "log.tmp");
            if (!temp.exists()) {
                temp.getParentFile().mkdirs();
                if (!temp.createNewFile()) {
                    throw new Exception("Failed to create file: " + temp.getPath());
                }
            }
            temp.deleteOnExit();
            FileWriter fileWriter = new FileWriter(temp);
            fileWriter.write(data);
            fileWriter.close();
            return temp;
        } catch (Exception e) {
            Static.error(e);
            Static.toast(this, getString(R.string.something_went_wrong));
            return null;
        }
    }

    private void display(){
        try {
            extraLog = Log.getExtraLog();
            ((TextView) findViewById(R.id.warn)).setText(String.valueOf(extraLog.warn));
            ((TextView) findViewById(R.id.error)).setText(String.valueOf(extraLog.error));
            ((TextView) findViewById(R.id.exception)).setText(String.valueOf(extraLog.exception));
            ((TextView) findViewById(R.id.wtf)).setText(String.valueOf(extraLog.wtf));
            ((TextView) findViewById(R.id.log_container)).setText(extraLog.log);
            SwipeRefreshLayout mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            if (mSwipeRefreshLayout != null) {
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                mSwipeRefreshLayout.setColorSchemeColors(Static.colorAccent);
                mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
        } catch (Exception e) {
            Static.error(e);
            Static.toast(this, getString(R.string.something_went_wrong));
        }
    }

}
