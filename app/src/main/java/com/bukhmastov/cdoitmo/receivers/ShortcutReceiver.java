package com.bukhmastov.cdoitmo.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ShortcutReceiverActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.activities.TimeRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONObject;

public class ShortcutReceiver extends BroadcastReceiver {

    private static final String TAG = "ShortcutReceiver";

    public static final String ACTION_CLICK_SHORTCUT = "com.bukhmastov.cdoitmo.CLICK_SHORTCUT";
    public static final String ACTION_ADD_SHORTCUT = "com.bukhmastov.cdoitmo.ADD_SHORTCUT";
    public static final String ACTION_REMOVE_SHORTCUT = "com.bukhmastov.cdoitmo.REMOVE_SHORTCUT";
    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String EXTRA_TYPE = "shortcut_type";
    public static final String EXTRA_DATA = "shortcut_data";

    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
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
                case ACTION_INSTALL_SHORTCUT: {
                    toast(context, context.getString(R.string.shortcut_created));
                    break;
                }
                default: {
                    Log.e(TAG, "Unsupported intent action: " + action);
                    break;
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void resolve(Context context, String shortcut_type, String shortcut_data){
        try {
            switch (shortcut_type) {
                case "tab": {
                    Intent intent = new Intent(context, SplashActivity.class);
                    intent.addFlags(Static.intentFlagRestart);
                    intent.putExtra("action", shortcut_data);
                    context.startActivity(intent);
                    break;
                }
                case "room101": {
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
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void addShortcut(Context context, String type, String data) {
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
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void installShortcut(Context context, String type, String data, String label, int icon){
        try {
            Intent shortcutIntent = new Intent(context, ShortcutReceiverActivity.class);
            shortcutIntent.setAction(ShortcutReceiver.ACTION_CLICK_SHORTCUT);
            shortcutIntent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
            shortcutIntent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
            Intent addIntent = new Intent(ShortcutReceiver.ACTION_INSTALL_SHORTCUT);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, icon));
            addIntent.putExtra("duplicate", false);
            context.sendBroadcast(addIntent);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void toast(Context context, String text){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
