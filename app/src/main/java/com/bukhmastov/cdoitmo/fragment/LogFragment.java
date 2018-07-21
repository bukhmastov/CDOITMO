package com.bukhmastov.cdoitmo.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.singleton.LogMetrics;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import java.io.File;
import java.io.FileWriter;

import javax.inject.Inject;

public class LogFragment extends ConnectedFragment {

    private static final String TAG = "LogFragment";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebaseCrashlyticsProvider firebaseCrashlyticsProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public void onViewCreated() {
        try {
            ((TextView) container.findViewById(R.id.warn)).setText(String.valueOf(LogMetrics.warn));
            ((TextView) container.findViewById(R.id.error)).setText(String.valueOf(LogMetrics.error));
            ((TextView) container.findViewById(R.id.exception)).setText(String.valueOf(LogMetrics.exception));
            ((TextView) container.findViewById(R.id.wtf)).setText(String.valueOf(LogMetrics.wtf));
            // init firebase logs enabler
            final ViewGroup firebase_logs = activity.findViewById(R.id.firebase_logs);
            final Switch firebase_logs_switch = activity.findViewById(R.id.firebase_logs_switch);
            firebase_logs.setOnClickListener(v -> thread.runOnUI(() -> {
                try {
                    firebase_logs_switch.setChecked(!storagePref.get(activity, "pref_allow_send_reports", true));
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            firebase_logs_switch.setChecked(storagePref.get(activity, "pref_allow_send_reports", true));
            firebase_logs_switch.setOnCheckedChangeListener((compoundButton, allowed) -> thread.run(() -> {
                try {
                    storagePref.put(activity, "pref_allow_send_reports", allowed);
                    firebaseToggled(allowed);
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            // init generic logs enabler
            final ViewGroup generic_logs = activity.findViewById(R.id.generic_logs);
            final Switch generic_logs_switch = activity.findViewById(R.id.generic_logs_switch);
            generic_logs.setOnClickListener(v -> thread.runOnUI(() -> {
                try {
                    generic_logs_switch.setChecked(!storagePref.get(activity, "pref_allow_collect_logs", false));
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            generic_logs_switch.setChecked(storagePref.get(activity, "pref_allow_collect_logs", false));
            generic_logs_switch.setOnCheckedChangeListener((compoundButton, allowed) -> thread.run(() -> {
                try {
                    storagePref.put(activity, "pref_allow_collect_logs", allowed);
                    genericToggled(allowed);
                    log.setEnabled(allowed);
                    if (allowed) {
                        log.i(TAG, "Logging has been enabled");
                    }
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            genericToggled(generic_logs_switch.isChecked());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_log;
    }

    @Override
    protected int getRootId() {
        return 0;
    }

    private void firebaseToggled(final boolean allowed) {
        thread.run(() -> firebaseCrashlyticsProvider.setEnabled(activity, allowed));
    }

    private void genericToggled(final boolean allowed) {
        thread.run(() -> {
            log.setEnabled(allowed);
            final ViewGroup generic = activity.findViewById(R.id.generic);
            final ViewGroup generic_send_logs = activity.findViewById(R.id.generic_send_logs);
            final ViewGroup generic_download_logs = activity.findViewById(R.id.generic_download_logs);
            final TextView log_container = activity.findViewById(R.id.log_container);
            if (allowed) {
                if (generic_send_logs != null) {
                    generic_send_logs.setOnClickListener(v -> thread.run(() -> {
                        try {
                            File logFile = getLogFile(log.getLog(false));
                            if (logFile != null) {
                                Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bukhmastov-alex@ya.ru"});
                                intent.putExtra(Intent.EXTRA_SUBJECT, "CDOITMO - log report");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setType(activity.getContentResolver().getType(tempUri));
                                intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.send_mail) + "..."));
                            }
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }));
                }
                if (generic_download_logs != null) {
                    generic_download_logs.setOnClickListener(v -> thread.run(() -> {
                        try {
                            File logFile = getLogFile(log.getLog(false));
                            if (logFile != null) {
                                Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setType(activity.getContentResolver().getType(tempUri));
                                intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share) + "..."));
                            }
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }));
                }
                thread.runOnUI(() -> {
                    if (generic != null) {
                        generic.setAlpha(1F);
                    }
                    if (log_container != null) {
                        log_container.setText(log.getLog());
                    }
                });
            } else {
                if (generic_send_logs != null) generic_send_logs.setOnClickListener(null);
                if (generic_download_logs != null) generic_download_logs.setOnClickListener(null);
                thread.runOnUI(() -> {
                    if (generic != null) {
                        generic.setAlpha(0.3F);
                    }
                    if (log_container != null) {
                        log_container.setText("");
                    }
                });
            }
        });
    }

    @Nullable
    private File getLogFile(String data) {
        try {
            File temp = new File(new File(activity.getCacheDir(), "shared"), "log.tmp");
            if (!temp.exists() && !temp.getParentFile().mkdirs() && !temp.createNewFile()) {
                throw new Exception("Failed to create file: " + temp.getPath());
            }
            temp.deleteOnExit();
            FileWriter fileWriter = new FileWriter(temp);
            fileWriter.write(data);
            fileWriter.close();
            return temp;
        } catch (Exception e) {
            log.exception(e);
            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            return null;
        }
    }
}
