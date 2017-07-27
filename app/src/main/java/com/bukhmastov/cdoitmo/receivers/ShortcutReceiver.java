package com.bukhmastov.cdoitmo.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.annotation.DrawableRes;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.DaysRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activities.ShortcutReceiverActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.activities.TimeRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONObject;

import java.util.Calendar;

public class ShortcutReceiver extends BroadcastReceiver {

    private static final String TAG = "ShortcutReceiver";

    public static final String ACTION_CLICK_SHORTCUT = "com.bukhmastov.cdoitmo.CLICK_SHORTCUT";
    public static final String ACTION_ADD_SHORTCUT = "com.bukhmastov.cdoitmo.ADD_SHORTCUT";
    public static final String ACTION_SHORTCUT_INSTALLED = "com.bukhmastov.cdoitmo.SHORTCUT_INSTALLED";
    public static final String ACTION_REMOVE_SHORTCUT = "com.bukhmastov.cdoitmo.REMOVE_SHORTCUT";
    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String EXTRA_TYPE = "shortcut_type";
    public static final String EXTRA_DATA = "shortcut_data";

    public void onReceive(final Context context, final Intent intent) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String action = intent.getAction();
                    Log.i(TAG, "onReceive | action=" + action);
                    switch (action) {
                        case ACTION_ADD_SHORTCUT: {
                            Bundle extras = intent.getExtras();
                            if (extras != null) {
                                String shortcut_type = extras.getString(ShortcutReceiver.EXTRA_TYPE);
                                String shortcut_data = extras.getString(ShortcutReceiver.EXTRA_DATA);
                                addShortcut(context, shortcut_type, shortcut_data);
                            } else {
                                throw new Exception("extras are null");
                            }
                            break;
                        }
                        case ACTION_REMOVE_SHORTCUT: break;
                        case ACTION_CLICK_SHORTCUT: {
                            Bundle extras = intent.getExtras();
                            if (extras != null) {
                                String shortcut_type = extras.getString(ShortcutReceiver.EXTRA_TYPE);
                                String shortcut_data = extras.getString(ShortcutReceiver.EXTRA_DATA);
                                resolve(context, shortcut_type, shortcut_data);
                            } else {
                                throw new Exception("extras are null");
                            }
                            break;
                        }
                        case ACTION_INSTALL_SHORTCUT:
                        case ACTION_SHORTCUT_INSTALLED: {
                            Static.toast(context, context.getString(R.string.shortcut_created));
                            break;
                        }
                        default: {
                            Log.e(TAG, "unsupported intent action: " + action);
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void resolve(final Context context, final String shortcut_type, final String shortcut_data) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "resolve | shortcut_type=" + shortcut_type + " | shortcut_data=" + shortcut_data);
                try {
                    FirebaseAnalyticsProvider.logEvent(
                            context,
                            FirebaseAnalyticsProvider.Event.SHORTCUT_USE,
                            FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.SHORTCUT_INFO, shortcut_type + "#" + shortcut_data)
                    );
                    switch (shortcut_type) {
                        case "tab": {
                            Intent intent = new Intent(context, SplashActivity.class);
                            intent.addFlags(Static.intentFlagRestart);
                            intent.putExtra("action", shortcut_data);
                            context.startActivity(intent);
                            break;
                        }
                        case "room101":
                        case "university": {
                            Intent intent = new Intent(context, SplashActivity.class);
                            intent.addFlags(Static.intentFlagRestart);
                            intent.putExtra("action", shortcut_type);
                            intent.putExtra("action_extra", shortcut_data);
                            context.startActivity(intent);
                            break;
                        }
                        case "schedule_lessons":
                        case "schedule_exams": {
                            Intent intent = new Intent(context, SplashActivity.class);
                            intent.addFlags(Static.intentFlagRestart);
                            intent.putExtra("action", shortcut_type);
                            intent.putExtra("action_extra", (new JSONObject(shortcut_data)).getString("query"));
                            context.startActivity(intent);
                            break;
                        }
                        case "time_remaining_widget": {
                            Intent intent = new Intent(context, TimeRemainingWidgetActivity.class);
                            intent.addFlags(Static.intentFlagRestart);
                            intent.putExtra("shortcut_data", shortcut_data);
                            context.startActivity(intent);
                            break;
                        }
                        case "days_remaining_widget": {
                            Intent intent = new Intent(context, DaysRemainingWidgetActivity.class);
                            intent.addFlags(Static.intentFlagRestart);
                            intent.putExtra("shortcut_data", shortcut_data);
                            context.startActivity(intent);
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void addShortcut(final Context context, final String type, final String data) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "addShortcut | type=" + type + " | data=" + data);
                try {
                    switch (type) {
                        case "tab": {
                            switch (data) {
                                case "e_journal":
                                    installShortcut(context, type, data, context.getString(R.string.e_journal), R.drawable.ic_shortcut_e_journal);
                                    break;
                                case "protocol_changes":
                                    installShortcut(context, type, data, context.getString(R.string.protocol_changes), R.drawable.ic_shortcut_protocol_changes);
                                    break;
                                case "rating":
                                    installShortcut(context, type, data, context.getString(R.string.rating), R.drawable.ic_shortcut_rating);
                                    break;
                                case "room101":
                                    installShortcut(context, type, data, context.getString(R.string.room101), R.drawable.ic_shortcut_room101);
                                    break;
                            }
                            break;
                        }
                        case "room101": {
                            installShortcut(context, type, data, context.getString(R.string.shortcut_room101_short), R.drawable.ic_shortcut_room101_add);
                            break;
                        }
                        case "schedule_lessons": {
                            JSONObject json = new JSONObject(data);
                            installShortcut(context, type, data, json.getString("label"), R.drawable.ic_shortcut_schedule_lessons);
                            break;
                        }
                        case "schedule_exams": {
                            JSONObject json = new JSONObject(data);
                            installShortcut(context, type, data, json.getString("label"), R.drawable.ic_shortcut_schedule_exams);
                            break;
                        }
                        case "time_remaining_widget": {
                            JSONObject json = new JSONObject(data);
                            installShortcut(context, type, data, json.getString("label"), R.drawable.ic_shortcut_time_remaining_widget);
                            break;
                        }
                        case "days_remaining_widget": {
                            JSONObject json = new JSONObject(data);
                            installShortcut(context, type, data, json.getString("label"), R.drawable.ic_shortcut_days_remaining_widget);
                            break;
                        }
                        case "university": {
                            JSONObject json = new JSONObject(data);
                            installShortcut(context, type, json.getString("query"), json.getString("label"), R.drawable.ic_shortcut_university);
                            break;
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private void installShortcut(final Context context, final String type, final String data, final String label, @DrawableRes final int icon) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "installShortcut | type=" + type + " | data=" + data);
                try {
                    Intent shortcutIntent = new Intent(context, ShortcutReceiverActivity.class);
                    shortcutIntent.setAction(ShortcutReceiver.ACTION_CLICK_SHORTCUT);
                    shortcutIntent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
                    shortcutIntent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                        if (shortcutManager.isRequestPinShortcutSupported()) {
                            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, "synthetic-" + Calendar.getInstance().getTimeInMillis())
                                .setIcon(Icon.createWithResource(context, icon))
                                .setShortLabel(label)
                                .setIntent(shortcutIntent)
                                .build();

                            Intent pinnedShortcutCallbackIntent = new Intent(context, ShortcutReceiver.class);
                            pinnedShortcutCallbackIntent.setAction(ShortcutReceiver.ACTION_SHORTCUT_INSTALLED);
                            IntentSender pinnedShortcutCallbackPendingIntentSender = PendingIntent.getBroadcast(context, 0, pinnedShortcutCallbackIntent, 0).getIntentSender();
                            shortcutManager.requestPinShortcut(pinShortcutInfo, pinnedShortcutCallbackPendingIntentSender);
                        } else {
                            Static.toast(context, context.getString(R.string.pin_shortcut_not_supported));
                        }
                    } else {
                        Intent addIntent = new Intent(ShortcutReceiver.ACTION_INSTALL_SHORTCUT);
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, icon));
                        addIntent.putExtra("duplicate", false);
                        context.sendBroadcast(addIntent);
                    }
                    FirebaseAnalyticsProvider.logEvent(
                            context,
                            FirebaseAnalyticsProvider.Event.SHORTCUT_INSTALL,
                            FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.SHORTCUT_INFO, type + "#" + data)
                    );
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }
}
