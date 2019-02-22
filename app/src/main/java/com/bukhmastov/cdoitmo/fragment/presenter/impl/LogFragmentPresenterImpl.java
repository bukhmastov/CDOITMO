package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.LogFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.LogMetrics;

import java.io.File;
import java.io.FileWriter;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import dagger.Lazy;

public class LogFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements LogFragmentPresenter {

    private static final String TAG = "LogFragment";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseCrashlyticsProvider firebaseCrashlyticsProvider;
    @Inject
    Lazy<Static> staticUtil;

    public LogFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem info = menu.findItem(R.id.action_preference_hard_reset);
            if (info != null) {
                info.setVisible(true);
                info.setOnMenuItemClickListener(item -> {
                    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                        return false;
                    }
                    new AlertDialog.Builder(activity)
                            .setIcon(R.drawable.ic_settings_restore_black)
                            .setTitle(R.string.hidden_preference_hard_reset)
                            .setMessage(R.string.hidden_preference_hard_reset_message)
                            .setNegativeButton(R.string.do_cancel, null)
                            .setPositiveButton(R.string.proceed, (dialog, which) -> hardResetPreferences())
                            .create().show();
                    return true;
                });
            }

        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public void onViewCreated() {
        thread.runOnUI(() -> {
            ((TextView) fragment.container().findViewById(R.id.warn)).setText(String.valueOf(LogMetrics.warn));
            ((TextView) fragment.container().findViewById(R.id.error)).setText(String.valueOf(LogMetrics.error));
            ((TextView) fragment.container().findViewById(R.id.exception)).setText(String.valueOf(LogMetrics.exception));
            ((TextView) fragment.container().findViewById(R.id.wtf)).setText(String.valueOf(LogMetrics.wtf));
            // init firebase logs enabler
            ViewGroup firebaseLogs = activity.findViewById(R.id.firebase_logs);
            Switch firebaseLogsSwitch = activity.findViewById(R.id.firebase_logs_switch);
            firebaseLogs.setOnClickListener(v -> thread.runOnUI(() -> {
                try {
                    firebaseLogsSwitch.setChecked(!storagePref.get(activity, "pref_allow_send_reports", true));
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            firebaseLogsSwitch.setChecked(storagePref.get(activity, "pref_allow_send_reports", true));
            firebaseLogsSwitch.setOnCheckedChangeListener((compoundButton, allowed) -> {
                try {
                    storagePref.put(activity, "pref_allow_send_reports", allowed);
                    firebaseToggled(allowed);
                } catch (Exception e) {
                    log.exception(e);
                }
            });
            // init generic logs enabler
            ViewGroup genericLogs = activity.findViewById(R.id.generic_logs);
            Switch genericLogsSwitch = activity.findViewById(R.id.generic_logs_switch);
            genericLogs.setOnClickListener(v -> thread.runOnUI(() -> {
                try {
                    genericLogsSwitch.setChecked(!storagePref.get(activity, "pref_allow_collect_logs", false));
                } catch (Exception e) {
                    log.exception(e);
                }
            }));
            genericLogsSwitch.setChecked(storagePref.get(activity, "pref_allow_collect_logs", false));
            genericLogsSwitch.setOnCheckedChangeListener((compoundButton, allowed) -> {
                thread.standalone(() -> {
                    storagePref.put(activity, "pref_allow_collect_logs", allowed);
                    genericToggled(allowed);
                    log.setEnabled(allowed);
                    if (allowed) {
                        log.i(TAG, "Logging has been enabled");
                    }
                }, throwable -> {
                    log.exception(throwable);
                });
            });
            genericToggled(genericLogsSwitch.isChecked());
        });
    }

    private void firebaseToggled(final boolean allowed) {
        thread.standalone(() -> firebaseCrashlyticsProvider.setEnabled(activity, allowed));
    }

    private void genericToggled(final boolean allowed) {
        thread.standalone(() -> {
            log.setEnabled(allowed);
            ViewGroup generic = activity.findViewById(R.id.generic);
            ViewGroup genericSendLogs = activity.findViewById(R.id.generic_send_logs);
            ViewGroup genericDownloadLogs = activity.findViewById(R.id.generic_download_logs);
            TextView logContainer = activity.findViewById(R.id.log_container);
            if (!allowed) {
                if (genericSendLogs != null) genericSendLogs.setOnClickListener(null);
                if (genericDownloadLogs != null) genericDownloadLogs.setOnClickListener(null);
                thread.runOnUI(() -> {
                    if (generic != null) {
                        generic.setAlpha(0.3F);
                    }
                    if (logContainer != null) {
                        logContainer.setText("");
                    }
                });
                return;
            }
            if (genericSendLogs != null) {
                genericSendLogs.setOnClickListener(v -> {
                    thread.standalone(() -> {
                        File logFile = getLogFile(log.getLog(false));
                        if (logFile != null) {
                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"bukhmastov-alex@ya.ru"});
                            intent.putExtra(Intent.EXTRA_SUBJECT, "CDOITMO - log report");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setType(activity.getContentResolver().getType(tempUri));
                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                            eventBus.fire(new OpenIntentEvent(Intent.createChooser(intent, activity.getString(R.string.send_mail) + "...")));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    });
                });
            }
            if (genericDownloadLogs != null) {
                genericDownloadLogs.setOnClickListener(v -> {
                    thread.standalone(() -> {
                        File logFile = getLogFile(log.getLog(false));
                        if (logFile != null) {
                            Uri tempUri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", logFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setType(activity.getContentResolver().getType(tempUri));
                            intent.putExtra(Intent.EXTRA_STREAM, tempUri);
                            eventBus.fire(new OpenIntentEvent(Intent.createChooser(intent, activity.getString(R.string.share) + "...")));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    });
                });
            }
            thread.runOnUI(() -> {
                if (generic != null) {
                    generic.setAlpha(1F);
                }
                if (logContainer != null) {
                    logContainer.setText(log.getLog());
                }
            });
        });
    }

    private void hardResetPreferences() {
        thread.standalone(() -> {
            if (activity == null) {
                return;
            }
            storagePref.hardResetUncategorized(activity);
            storagePref.applyDebug(activity);
            notificationMessage.snackBar(
                    activity,
                    activity.getString(R.string.restart_required),
                    activity.getString(R.string.restart),
                    NotificationMessage.LENGTH_LONG,
                    view -> staticUtil.get().reLaunch(activity)
            );
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

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
