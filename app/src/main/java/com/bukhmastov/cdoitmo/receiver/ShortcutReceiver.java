package com.bukhmastov.cdoitmo.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import androidx.annotation.DrawableRes;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.DaysRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.ShortcutReceiverActivity;
import com.bukhmastov.cdoitmo.activity.TimeRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.model.entity.ShortcutQuery;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

public class ShortcutReceiver extends BroadcastReceiver {

    private static final String TAG = "ShortcutReceiver";

    public static final String ACTION_CLICK_SHORTCUT = "com.bukhmastov.cdoitmo.CLICK_SHORTCUT";
    public static final String ACTION_ADD_SHORTCUT = "com.bukhmastov.cdoitmo.ADD_SHORTCUT";
    public static final String ACTION_SHORTCUT_INSTALLED = "com.bukhmastov.cdoitmo.SHORTCUT_INSTALLED";
    public static final String ACTION_REMOVE_SHORTCUT = "com.bukhmastov.cdoitmo.REMOVE_SHORTCUT";
    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String EXTRA_TYPE = "shortcut_type";
    public static final String EXTRA_DATA = "shortcut_data";
    public static final String EXTRA_MODE = "shortcut_mode";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        AppComponentProvider.getComponent().inject(this);
        thread.standalone(() -> {
            String action = intent.getAction();
            log.i(TAG, "onReceive | action=", action);
            switch (StringUtils.emptyIfNull(action)) {
                case ACTION_ADD_SHORTCUT: {
                    Bundle extras = intent.getExtras();
                    if (extras == null) {
                        throw new Exception("Extras are null");
                    }
                    String shortcutType = extras.getString(ShortcutReceiver.EXTRA_TYPE);
                    String shortcutData = extras.getString(ShortcutReceiver.EXTRA_DATA);
                    String shortcutMode = extras.getString(ShortcutReceiver.EXTRA_MODE);
                    addShortcut(context, shortcutType, shortcutData, shortcutMode);
                    break;
                }
                case ACTION_REMOVE_SHORTCUT: break;
                case ACTION_CLICK_SHORTCUT: {
                    Bundle extras = intent.getExtras();
                    if (extras == null) {
                        throw new Exception("Extras are null");
                    }
                    String shortcutType = extras.getString(ShortcutReceiver.EXTRA_TYPE);
                    String shortcutData = extras.getString(ShortcutReceiver.EXTRA_DATA);
                    String shortcutMode = extras.getString(ShortcutReceiver.EXTRA_MODE);
                    resolve(context, shortcutType, shortcutData, StringUtils.defaultIfBlank(shortcutMode, "regular"));
                    break;
                }
                case ACTION_INSTALL_SHORTCUT:
                case ACTION_SHORTCUT_INSTALLED: {
                    notificationMessage.toast(context, context.getString(R.string.shortcut_created));
                    break;
                }
                default: {
                    log.e(TAG, "Unsupported intent action: ", action);
                    break;
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void resolve(Context context, String shortcutType, String shortcutData, String shortcutMode) throws Exception {
        log.v(TAG, "resolve | shortcutType=", shortcutType, " | shortcutData=", shortcutData, " | shortcutMode=", shortcutMode);
        firebaseAnalyticsProvider.logEvent(
                context,
                FirebaseAnalyticsProvider.Event.SHORTCUT_USE,
                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.SHORTCUT_TYPE, shortcutType)
        );
        switch (shortcutType) {
            case "offline": {
                Bundle extras = new Bundle();
                extras.putString("mode", "offline");
                eventBus.fire(new OpenActivityEvent(MainActivity.class, extras, App.intentFlagRestart));
                break;
            }
            case "tab": {
                Bundle extras = new Bundle();
                extras.putString("mode", shortcutMode);
                extras.putString("action", shortcutData);
                eventBus.fire(new OpenActivityEvent(MainActivity.class, extras, App.intentFlagRestart));
                break;
            }
            case "room101":
            case "university": {
                Bundle extras = new Bundle();
                extras.putString("mode", shortcutMode);
                extras.putString("action", shortcutType);
                extras.putString("action_extra", shortcutData);
                eventBus.fire(new OpenActivityEvent(MainActivity.class, extras, App.intentFlagRestart));
                break;
            }
            case "schedule_lessons":
            case "schedule_exams":
            case "schedule_attestations": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(shortcutData);
                Bundle extras = new Bundle();
                extras.putString("mode", shortcutMode);
                extras.putString("action", shortcutType);
                extras.putString("action_extra", shortcutQuery.getQuery());
                eventBus.fire(new OpenActivityEvent(MainActivity.class, extras, App.intentFlagRestart));
                break;
            }
            case "time_remaining_widget": {
                Bundle extras = new Bundle();
                extras.putString("mode", shortcutMode);
                extras.putString("shortcut_data", shortcutData);
                eventBus.fire(new OpenActivityEvent(TimeRemainingWidgetActivity.class, extras, App.intentFlagRestart));
                break;
            }
            case "days_remaining_widget": {
                Bundle extras = new Bundle();
                extras.putString("mode", shortcutMode);
                extras.putString("shortcut_data", shortcutData);
                eventBus.fire(new OpenActivityEvent(DaysRemainingWidgetActivity.class, extras, App.intentFlagRestart));
                break;
            }
        }
    }

    private void addShortcut(Context context, String type, String data, String mode) throws Exception {
        log.v(TAG, "addShortcut | type=", type, " | data=", data);
        switch (type) {
            case "offline": {
                installShortcut(context, type, data, mode, context.getString(R.string.app_name), R.mipmap.ic_shortcut_offline);
                break;
            }
            case "tab": {
                switch (data) {
                    case "e_journal":
                        installShortcut(context, type, data, mode, context.getString(R.string.e_journal), R.mipmap.ic_shortcut_e_journal);
                        break;
                    case "protocol_changes":
                        installShortcut(context, type, data, mode, context.getString(R.string.protocol_changes), R.mipmap.ic_shortcut_protocol_changes);
                        break;
                    case "rating":
                        installShortcut(context, type, data, mode, context.getString(R.string.rating), R.mipmap.ic_shortcut_rating);
                        break;
                    case "room101":
                        installShortcut(context, type, data, mode, context.getString(R.string.room101), R.mipmap.ic_shortcut_room101);
                        break;
                    case "groups":
                        installShortcut(context, type, data, mode, context.getString(R.string.study_groups), R.mipmap.ic_shortcut_groups);
                        break;
                    case "scholarship":
                        installShortcut(context, type, data, mode, context.getString(R.string.scholarship), R.mipmap.ic_shortcut_scholarship);
                        break;
                }
                break;
            }
            case "room101": {
                installShortcut(context, type, data, mode, context.getString(R.string.shortcut_room101_short), R.mipmap.ic_shortcut_room101_add);
                break;
            }
            case "schedule_lessons": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, data, mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_schedule_lessons);
                break;
            }
            case "schedule_exams": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, data, mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_schedule_exams);
                break;
            }
            case "schedule_attestations": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, data, mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_schedule_attestations);
                break;
            }
            case "time_remaining_widget": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, data, mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_time_remaining_widget);
                break;
            }
            case "days_remaining_widget": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, data, mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_days_remaining_widget);
                break;
            }
            case "university": {
                ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(data);
                installShortcut(context, type, shortcutQuery.getQuery(), mode, shortcutQuery.getLabel(), R.mipmap.ic_shortcut_university);
                break;
            }
        }
    }

    private void installShortcut(Context context, String type, String data, String mode, String label, @DrawableRes int icon) {
        log.v(TAG, "installShortcut | type=", type, " | data=", data);
        Intent intent = new Intent(context, ShortcutReceiverActivity.class);
        intent.setAction(ShortcutReceiver.ACTION_CLICK_SHORTCUT);
        intent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
        intent.putExtra(ShortcutReceiver.EXTRA_MODE, mode);
        intent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported()) {
                notificationMessage.toast(context, context.getString(R.string.pin_shortcut_not_supported));
                return;
            }
            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, "synthetic-" + time.getTimeInMillis())
                .setIcon(Icon.createWithResource(context, icon))
                .setShortLabel(label)
                .setIntent(intent)
                .build();
            Intent pinnedShortcutCallbackIntent = new Intent(context, ShortcutReceiver.class);
            pinnedShortcutCallbackIntent.setAction(ShortcutReceiver.ACTION_SHORTCUT_INSTALLED);
            IntentSender pinnedShortcutCallbackPendingIntentSender = PendingIntent.getBroadcast(context, 0, pinnedShortcutCallbackIntent, 0).getIntentSender();
            shortcutManager.requestPinShortcut(pinShortcutInfo, pinnedShortcutCallbackPendingIntentSender);
        } else {
            Intent addIntent = new Intent(ShortcutReceiver.ACTION_INSTALL_SHORTCUT);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, icon));
            addIntent.putExtra("duplicate", false);
            context.sendBroadcast(addIntent);
        }
        firebaseAnalyticsProvider.logEvent(
                context,
                FirebaseAnalyticsProvider.Event.SHORTCUT_INSTALL,
                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.SHORTCUT_TYPE, type)
        );
    }
}
