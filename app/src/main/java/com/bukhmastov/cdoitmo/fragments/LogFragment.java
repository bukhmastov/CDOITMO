package com.bukhmastov.cdoitmo.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.io.File;
import java.io.FileWriter;

public class LogFragment extends ConnectedFragment {

    private static final String TAG = "LogFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            ((TextView) view.findViewById(R.id.warn)).setText(String.valueOf(Log.Metrics.warn));
            ((TextView) view.findViewById(R.id.error)).setText(String.valueOf(Log.Metrics.error));
            ((TextView) view.findViewById(R.id.exception)).setText(String.valueOf(Log.Metrics.exception));
            ((TextView) view.findViewById(R.id.wtf)).setText(String.valueOf(Log.Metrics.wtf));
            // init firebase logs enabler
            final ViewGroup firebase_logs = activity.findViewById(R.id.firebase_logs);
            final Switch firebase_logs_switch = activity.findViewById(R.id.firebase_logs_switch);
            firebase_logs.setOnClickListener(v -> Static.T.runOnUiThread(() -> {
                try {
                    firebase_logs_switch.setChecked(!Storage.pref.get(activity, "pref_allow_send_reports", true));
                } catch (Exception e) {
                    Static.error(e);
                }
            }));
            firebase_logs_switch.setChecked(Storage.pref.get(activity, "pref_allow_send_reports", true));
            firebase_logs_switch.setOnCheckedChangeListener((compoundButton, allowed) -> Static.T.runThread(() -> {
                try {
                    Storage.pref.put(activity, "pref_allow_send_reports", allowed);
                    firebaseToggled(allowed);
                } catch (Exception e) {
                    Static.error(e);
                }
            }));
            // init generic logs enabler
            final ViewGroup generic_logs = activity.findViewById(R.id.generic_logs);
            final Switch generic_logs_switch = activity.findViewById(R.id.generic_logs_switch);
            generic_logs.setOnClickListener(v -> Static.T.runOnUiThread(() -> {
                try {
                    generic_logs_switch.setChecked(!Storage.pref.get(activity, "pref_allow_collect_logs", false));
                } catch (Exception e) {
                    Static.error(e);
                }
            }));
            generic_logs_switch.setChecked(Storage.pref.get(activity, "pref_allow_collect_logs", false));
            generic_logs_switch.setOnCheckedChangeListener((compoundButton, allowed) -> Static.T.runThread(() -> {
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
            }));
            genericToggled(generic_logs_switch.isChecked());
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void firebaseToggled(final boolean allowed) {
        Static.T.runThread(() -> FirebaseCrashlyticsProvider.setEnabled(activity, allowed));
    }
    private void genericToggled(final boolean allowed) {
        Static.T.runThread(() -> {
            Log.setEnabled(allowed);
            final ViewGroup generic = activity.findViewById(R.id.generic);
            final ViewGroup generic_send_logs = activity.findViewById(R.id.generic_send_logs);
            final ViewGroup generic_download_logs = activity.findViewById(R.id.generic_download_logs);
            final TextView log_container = activity.findViewById(R.id.log_container);
            if (allowed) {
                generic_send_logs.setOnClickListener(v -> Static.T.runThread(() -> {
                    try {
                        File logFile = getLogFile(Log.getLog(false));
                        if (logFile != null) {
                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bukhmastov-alex@ya.ru"});
                            intent.putExtra(Intent.EXTRA_SUBJECT, "CDOITMO - log report");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setType(activity.getContentResolver().getType(tempUri));
                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                            startActivity(Intent.createChooser(intent, activity.getString(R.string.send_mail) + "..."));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        Static.toast(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                generic_download_logs.setOnClickListener(v -> Static.T.runThread(() -> {
                    try {
                        File logFile = getLogFile(Log.getLog(false));
                        if (logFile != null) {
                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setType(activity.getContentResolver().getType(tempUri));
                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                            startActivity(Intent.createChooser(intent, activity.getString(R.string.share) + "..."));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        Static.toast(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                Static.T.runOnUiThread(() -> {
                    generic.setAlpha(1F);
                    log_container.setText(Log.getLog());
                });
            } else {
                generic_send_logs.setOnClickListener(null);
                generic_download_logs.setOnClickListener(null);
                Static.T.runOnUiThread(() -> {
                    generic.setAlpha(0.3F);
                    log_container.setText("");
                });
            }
        });
    }

    @Nullable
    private File getLogFile(String data) {
        try {
            File temp = new File(new File(activity.getCacheDir(), "shared"), "log.tmp");
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
            Static.toast(activity, activity.getString(R.string.something_went_wrong));
            return null;
        }
    }
}
