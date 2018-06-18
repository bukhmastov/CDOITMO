package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.activity.MainActivity;

import java.util.UUID;

public class Static {

    private static final String TAG = "Static";
    public static final String GLITCH = "%*<@?!";

    public static String getUUID(Context context) {
        String uuid = Storage.pref.get(context, "pref_uuid", "");
        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
            Storage.pref.put(context, "pref_uuid", uuid);
        }
        return uuid;
    }

    public static void reLaunch(Context context) {
        Log.i(TAG, "reLaunch");
        if (context == null) {
            Log.w(TAG, "reLaunch | context is null");
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(App.intentFlagRestart);
        context.startActivity(intent);
    }

    public static void hardReset(final Context context) {
        Log.i(TAG, "hardReset");
        if (context == null) {
            Log.w(TAG, "hardReset | context is null");
            return;
        }
        Account.logoutPermanently(context, () -> {
            Storage.file.all.reset(context);
            App.firstLaunch = true;
            App.OFFLINE_MODE = false;
            MainActivity.loaded = false;
            Static.reLaunch(context);
        });
    }

    public static void lockOrientation(Activity activity, boolean lock) {
        try {
            if (activity != null) {
                Log.v(TAG, "lockOrientation | activity=", activity.getComponentName().getClassName(), " | lock=", lock);
                activity.setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    public static void removeView(final View view) {
        Thread.runOnUI(() -> {
            try {
                ((ViewGroup) view.getParent()).removeView(view);
            } catch (Throwable e) {
                Log.exception(e);
            }
        });
    }

    public static <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (element == value || value != null && value.equals(element)) return true;
        }
        return false;
    }
}
