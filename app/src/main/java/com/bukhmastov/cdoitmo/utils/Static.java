package com.bukhmastov.cdoitmo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.objects.entities.ScheduleMenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Static {

    public static ErrorTracker errorTracker;
    public static String versionName;
    public static int versionCode;
    public static int textColorPrimary, textColorSecondary, colorSeparator, colorBackgroundSnackBar, colorAccent, colorBackgroundRefresh;
    public static float destiny;
    public static TypedValue typedValue;
    public static boolean OFFLINE_MODE = false;
    public static int week = -1;
    public static boolean firstLaunch = true;
    public static boolean authorized = false;
    public static ProtocolTracker protocolTracker = null;
    public static boolean darkTheme = false;
    public static int intentFlagRestart = 268468224;
    public static SparseArray<ScheduleMenuItem> scheduleMenuItems = new SparseArray<>();

    public static void init(Context context) {
        if (Static.errorTracker == null) {
            Static.errorTracker = new ErrorTracker();
        }
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Static.versionName = pInfo.versionName;
            Static.versionCode = pInfo.versionCode;
            typedValue = new TypedValue();
            textColorPrimary = resolveColor(context, android.R.attr.textColorPrimary);
            textColorSecondary = resolveColor(context, android.R.attr.textColorSecondary);
            colorSeparator = resolveColor(context, R.attr.colorSeparator);
            colorBackgroundSnackBar = resolveColor(context, R.attr.colorBackgroundSnackBar);
            colorAccent = resolveColor(context, R.attr.colorAccent);
            colorBackgroundRefresh = resolveColor(context, R.attr.colorBackgroundRefresh);
            destiny = context.getResources().getDisplayMetrics().density;
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public static void error(Throwable throwable){
        if (Static.errorTracker == null) {
            Static.errorTracker = new ErrorTracker();
        }
        Static.errorTracker.add(throwable);
    }
    public static boolean isOnline(Context context) {
        if(context != null) {
            NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return true;
        }
    }
    public static void reLaunch(Context context){
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Static.intentFlagRestart);
        context.startActivity(intent);
    }
    public static void hardReset(Context context){
        Static.logout(context);
        Static.firstLaunch = true;
        Static.OFFLINE_MODE = false;
        MainActivity.loaded = false;
        Static.reLaunch(context);
    }
    public static int resolveColor(Context context, int reference) throws Exception {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(reference, typedValue, true);
        //return typedValue.data;
        return context.obtainStyledAttributes(typedValue.data, new int[]{reference}).getColor(0, -1);
    }
    public static void updateWeek(Context context){
        try {
            String weekStr = Storage.file.perm.get(context, "user#week");
            if(!Objects.equals(weekStr, "")){
                JSONObject jsonObject = new JSONObject(weekStr);
                int week = jsonObject.getInt("week");
                if (week >= 0){
                    Calendar past = Calendar.getInstance();
                    past.setTimeInMillis(jsonObject.getLong("timestamp"));
                    Static.week = week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                }
            }
        } catch (JSONException e) {
            Static.error(e);
            Storage.file.perm.delete(context, "user#week");
        }
    }
    public static void logout(Context context){
        new ProtocolTracker(context).stop();
        Storage.file.all.clear(context);
        Static.logoutCurrent(context);
        Static.authorized = false;
    }
    public static void logoutCurrent(Context context){
        new ProtocolTracker(context).stop();
        Storage.file.general.delete(context, "users#current_login");
    }
    public static void lockOrientation(Activity activity, boolean lock){
        if (activity != null) {
            activity.setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }
    public static void showUpdateTime(Activity activity, long time, int layoutId, boolean show_now){
        if (activity == null) return;
        String message = getUpdateTime(activity, time);
        int shift = (int) ((Calendar.getInstance().getTimeInMillis() - time) / 1000L);
        if (show_now || shift > 4) {
            View layout = activity.findViewById(layoutId);
            if (layout != null) {
                Snackbar snackbar = Snackbar.make(layout, activity.getString(R.string.update_date) + " " + message, Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
                snackbar.show();
            }
        }
    }
    public static String getUpdateTime(Activity activity, long time) {
        int shift = (int) ((Calendar.getInstance().getTimeInMillis() - time) / 1000L);
        String message;
        if (shift < 21600) {
            if (shift < 5) {
                message = activity.getString(R.string.right_now);
            } else if (shift < 60) {
                message = shift + " " + activity.getString(R.string.sec_past);
            } else if (shift < 3600) {
                message = shift / 60 + " " + activity.getString(R.string.min_past);
            } else {
                message = shift / 3600 + " " + activity.getString(R.string.hour_past);
            }
        } else {
            message = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(time));
        }
        return message;
    }
    public static String getGenitiveMonth(Context context, String month) {
        switch (month) {
            case "01": month = context.getString(R.string.january_genitive); break;
            case "02": month = context.getString(R.string.february_genitive); break;
            case "03": month = context.getString(R.string.march_genitive); break;
            case "04": month = context.getString(R.string.april_genitive); break;
            case "05": month = context.getString(R.string.may_genitive); break;
            case "06": month = context.getString(R.string.june_genitive); break;
            case "07": month = context.getString(R.string.july_genitive); break;
            case "08": month = context.getString(R.string.august_genitive); break;
            case "09": month = context.getString(R.string.september_genitive); break;
            case "10": month = context.getString(R.string.october_genitive); break;
            case "11": month = context.getString(R.string.november_genitive); break;
            case "12": month = context.getString(R.string.december_genitive); break;
        }
        return month;
    }
    public static String crypt(String value) {
        return crypt(value, "SHA-256");
    }
    public static String crypt(String value, String algorithm){
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = md.digest(value.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            switch (algorithm) {
                case "SHA-256":
                    hash = crypt(value, "SHA-1");
                    break;
                case "SHA-1":
                    hash = crypt(value, "MD5");
                    break;
                case "MD5":
                    Static.error(e);
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            Static.error(e);
        }
        return hash;
    }
    public static void delay(final Activity activity, final int sleep, final Runnable runnable){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                activity.runOnUiThread(runnable);
            }
        }).start();
    }

}




