package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.io.File;
import java.io.FileWriter;

public class LogActivity extends AppCompatActivity {

    private static final String TAG = "LogActivity";
    private Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_log);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_log));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        display();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    private void init() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // init firebase logs enabler
                    final ViewGroup firebase_logs = activity.findViewById(R.id.firebase_logs);
                    final Switch firebase_logs_switch = activity.findViewById(R.id.firebase_logs_switch);
                    firebase_logs.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        firebase_logs_switch.setChecked(!Storage.pref.get(activity, "pref_allow_send_reports", true));
                                    } catch (Exception e) {
                                        Static.error(e);
                                    }
                                }
                            });
                        }
                    });
                    firebase_logs_switch.setChecked(Storage.pref.get(activity, "pref_allow_send_reports", true));
                    firebase_logs_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, final boolean allowed) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Storage.pref.put(activity, "pref_allow_send_reports", allowed);
                                        firebaseToggled(allowed);
                                    } catch (Exception e) {
                                        Static.error(e);
                                    }
                                }
                            });
                        }
                    });
                    // init generic logs enabler
                    final ViewGroup generic_logs = activity.findViewById(R.id.generic_logs);
                    final Switch generic_logs_switch = activity.findViewById(R.id.generic_logs_switch);
                    generic_logs.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        generic_logs_switch.setChecked(!Storage.pref.get(activity, "pref_allow_collect_logs", false));
                                    } catch (Exception e) {
                                        Static.error(e);
                                    }
                                }
                            });
                        }
                    });
                    generic_logs_switch.setChecked(Storage.pref.get(activity, "pref_allow_collect_logs", false));
                    generic_logs_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, final boolean allowed) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Storage.pref.put(activity, "pref_allow_collect_logs", allowed);
                                        genericToggled(allowed);
                                        Log.setEnabled(allowed);
                                        if (allowed) {
                                            Log.i(TAG, "Logging has been enabled");
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                    }
                                }
                            });
                        }
                    });
                    genericToggled(generic_logs_switch.isChecked());
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void display() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ((TextView) activity.findViewById(R.id.warn)).setText(String.valueOf(Log.Metrics.warn));
                    ((TextView) activity.findViewById(R.id.error)).setText(String.valueOf(Log.Metrics.error));
                    ((TextView) activity.findViewById(R.id.exception)).setText(String.valueOf(Log.Metrics.exception));
                    ((TextView) activity.findViewById(R.id.wtf)).setText(String.valueOf(Log.Metrics.wtf));
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void firebaseToggled(final boolean allowed) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                FirebaseCrashProvider.setEnabled(activity, allowed, true);
            }
        });
    }

    private void genericToggled(final boolean allowed) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.setEnabled(allowed);
                final ViewGroup generic = activity.findViewById(R.id.generic);
                final ViewGroup generic_send_logs = activity.findViewById(R.id.generic_send_logs);
                final ViewGroup generic_download_logs = activity.findViewById(R.id.generic_download_logs);
                final TextView log_container = activity.findViewById(R.id.log_container);
                if (allowed) {
                    generic_send_logs.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        File logFile = getLogFile(Log.getLog(false));
                                        if (logFile != null) {
                                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bukhmastov-alex@ya.ru"});
                                            intent.putExtra(Intent.EXTRA_SUBJECT, "CDOITMO - log report");
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            intent.setType(getContentResolver().getType(tempUri));
                                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                                            startActivity(Intent.createChooser(intent, activity.getString(R.string.send_mail) + "..."));
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.toast(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                    generic_download_logs.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        File logFile = getLogFile(Log.getLog(false));
                                        if (logFile != null) {
                                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            intent.setType(getContentResolver().getType(tempUri));
                                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                                            startActivity(Intent.createChooser(intent, activity.getString(R.string.share) + "..."));
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.toast(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            generic.setAlpha(1F);
                            log_container.setText(Log.getLog());
                        }
                    });
                } else {
                    generic_send_logs.setOnClickListener(null);
                    generic_download_logs.setOnClickListener(null);
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            generic.setAlpha(0.3F);
                            log_container.setText("");
                        }
                    });
                }
            }
        });
    }

    @Nullable
    private File getLogFile(String data) {
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
            Static.toast(this, activity.getString(R.string.something_went_wrong));
            return null;
        }
    }
}
