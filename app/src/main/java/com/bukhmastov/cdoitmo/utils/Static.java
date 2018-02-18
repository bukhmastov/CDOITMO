package com.bukhmastov.cdoitmo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Static {

    private static final String TAG = "Static";
    public static String versionName;
    public static int versionCode;
    public static int textColorPrimary, textColorSecondary, colorSeparator, colorBackgroundSnackBar, colorAccent, colorBackgroundRefresh;
    public static boolean OFFLINE_MODE = false;
    public static boolean UNAUTHORIZED_MODE = false;
    public static boolean firstLaunch = true;
    public static final int intentFlagRestart = Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK;
    public static boolean tablet = false;
    public static boolean isFirstLaunchEver = false;
    public static boolean showIntroducingActivity = false;
    private static final String USER_AGENT_TEMPLATE = "CDOITMO/{versionName}/{versionCode} Java/Android/{sdkInt}";
    private static String USER_AGENT = null;
    private static String app_theme = null;
    public static final String GLITCH = "%*<@?!";

    public interface SimpleCallback {
        void onCall();
    }
    public interface StringCallback {
        void onCall(String data);
    }

    public static class T {
        private static final String TAG = "Static.T";
        private static final boolean DEBUG = false;
        public enum TYPE {FOREGROUND, BACKGROUND}
        private static class Th {
            public final TYPE type;
            public HandlerThread thread = null;
            public final String thread_name;
            public final int thread_priority;
            public Th(TYPE type, String thread_name, int thread_priority) {
                this.type = type;
                this.thread_name = thread_name;
                this.thread_priority = thread_priority;
            }
        }
        private static final Th Foreground = new Th(TYPE.FOREGROUND, "CDOExecutorForeground", Process.THREAD_PRIORITY_FOREGROUND);
        private static final Th Background = new Th(TYPE.BACKGROUND, "CDOExecutorBackground", Process.THREAD_PRIORITY_BACKGROUND);

        public static void runThread(final Runnable runnable) {
            runThread(TYPE.FOREGROUND, runnable);
        }
        public static void runThread(final TYPE type, final Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException("Passed runnable is null");
            }
            final Th th = getThread(type);
            if (th.thread != null && !th.thread.isAlive()) {
                log("runThread | HandlerThread is not alive, going to quit | id = " + th.thread.getId() + " | name = " + th.thread.getName());
                Looper looper = th.thread.getLooper();
                if (looper != null) {
                    looper.quit();
                }
                try {
                    th.thread.interrupt();
                } catch (Throwable ignore) {
                    // just ignore
                }
                th.thread = null;
            }
            if (th.thread == null) {
                th.thread = new HandlerThread(th.thread_name, th.thread_priority);
                th.thread.start();
                log("runThread | initialized new HandlerThread | id = " + th.thread.getId() + " | name = " + th.thread.getName());
            }
            log("runThread | run with Handler.post | id = " + th.thread.getId() + " | name = " + th.thread.getName());
            try {
                new Handler(th.thread.getLooper()).post(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable throwable) {
                        Log.exception("Run on " + th.thread.getName() + " thread failed", throwable);
                    }
                });
            } catch (Throwable throwable) {
                Log.exception("Run on " + th.thread.getName() + " thread failed", throwable);
            }
        }
        public static void runOnUiThread(final Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException("Passed runnable is null");
            }
            if (isMainThread()) {
                log("runOnUiThread | run on current thread");
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    Log.exception("Run on main thread failed", throwable);
                }
            } else {
                log("runOnUiThread | run with Handler.post");
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            runnable.run();
                        } catch (Throwable throwable) {
                            Log.exception("Run on main thread failed", throwable);
                        }
                    });
                } catch (Throwable throwable) {
                    Log.exception("Run on main thread failed", throwable);
                }
            }
        }
        public static boolean isMainThread() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : Thread.currentThread() == Looper.getMainLooper().getThread();
        }
        public static boolean isLooperThread() {
            return Thread.currentThread() == Foreground.thread || Thread.currentThread() == Background.thread || isMainThread();
        }
        private static Th getThread(TYPE type) {
            switch (type) {
                case BACKGROUND: return Background;
                case FOREGROUND: default: return Foreground;
            }
        }
        private static void log(String log) {
            if (DEBUG) {
                android.util.Log.v(TAG, log);
            }
        }
    }
    public static void init(Activity activity) {
        Log.i(TAG, "init");
        if (activity == null) {
            Log.w(TAG, "init | activity is null");
            return;
        }
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            Static.versionName = pInfo.versionName;
            Static.versionCode = pInfo.versionCode;
            textColorPrimary = resolveColor(activity, android.R.attr.textColorPrimary);
            textColorSecondary = resolveColor(activity, android.R.attr.textColorSecondary);
            colorSeparator = resolveColor(activity, R.attr.colorSeparator);
            colorBackgroundSnackBar = resolveColor(activity, R.attr.colorBackgroundSnackBar);
            colorAccent = resolveColor(activity, R.attr.colorAccent);
            colorBackgroundRefresh = resolveColor(activity, R.attr.colorBackgroundRefresh);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public static void error(Throwable throwable) {
        Log.exception(throwable);
    }
    public static String getUUID(Context context) {
        String uuid = Storage.pref.get(context, "pref_uuid", "");
        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
            Storage.pref.put(context, "pref_uuid", uuid);
        }
        return uuid;
    }
    public static boolean isOnline(Context context) {
        Log.v(TAG, "isOnline");
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return (networkInfo != null && networkInfo.isConnected());
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public static void reLaunch(Context context) {
        Log.i(TAG, "reLaunch");
        if (context == null) {
            Log.w(TAG, "reLaunch | context is null");
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Static.intentFlagRestart);
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
            Static.firstLaunch = true;
            Static.OFFLINE_MODE = false;
            MainActivity.loaded = false;
            Static.reLaunch(context);
        });
    }
    public static int resolveColor(Context context, int reference) throws Exception {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(reference, typedValue, true);
        return context.obtainStyledAttributes(typedValue.data, new int[]{reference}).getColor(0, -1);
    }
    public static Calendar getCalendar() {
        return Calendar.getInstance(Locale.GERMANY);
    }
    public static int getWeek(Context context) {
        return getWeek(context, getCalendar());
    }
    public static int getWeek(Context context, Calendar calendar) {
        int week = -1;
        long ts = 0;
        try {
            final String override = Storage.pref.get(context, "pref_week_force_override", "");
            if (!override.isEmpty()) {
                try {
                    String[] v = override.split("#");
                    if (v.length == 2) {
                        week = Integer.parseInt(v[0]);
                        ts = Long.parseLong(v[1]);
                    }
                } catch (Exception ignore) {/* ignore */}
            }
            if (week < 0) {
                final String stored = Storage.file.general.perm.get(context, "user#week").trim();
                if (!stored.isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(stored);
                        week = json.getInt("week");
                        ts = json.getLong("timestamp");
                    } catch (Exception e) {
                        Storage.file.general.perm.delete(context, "user#week");
                    }
                }
            }
            if (week >= 0) {
                final Calendar past = (Calendar) calendar.clone();
                past.setTimeInMillis(ts);
                return week + (calendar.get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
            }
        } catch (Exception ignore) {/* ignore */}
        return week;
    }
    public static int getWeekDay() {
        return getWeekDay(getCalendar());
    }
    public static int getWeekDay(Calendar calendar) {
        int weekday = 0;
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: weekday = 0; break;
            case Calendar.TUESDAY: weekday = 1; break;
            case Calendar.WEDNESDAY: weekday = 2; break;
            case Calendar.THURSDAY: weekday = 3; break;
            case Calendar.FRIDAY: weekday = 4; break;
            case Calendar.SATURDAY: weekday = 5; break;
            case Calendar.SUNDAY: weekday = 6; break;
        }
        return weekday;
    }
    public static void lockOrientation(Activity activity, boolean lock) {
        try {
            if (activity != null) {
                Log.v(TAG, "lockOrientation | activity=", activity.getComponentName().getClassName(), " | lock=", lock);
                activity.setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public static void showUpdateTime(Activity activity, long time, boolean show_now) {
        showUpdateTime(activity, android.R.id.content, time, show_now);
    }
    public static void showUpdateTime(Activity activity, @IdRes int layout, long time, boolean show_now) {
        String message = getUpdateTime(activity, time);
        int shift = (int) ((getCalendar().getTimeInMillis() - time) / 1000L);
        if (show_now || shift > 4) {
            Static.snackBar(activity, layout, activity.getString(R.string.update_date) + " " + message);
        }
    }
    public static String getUpdateTime(Context context, long time) {
        int shift = (int) ((getCalendar().getTimeInMillis() - time) / 1000L);
        String message;
        if (shift < 21600 && context != null) {
            if (shift < 5) {
                message = context.getString(R.string.right_now);
            } else if (shift < 60) {
                message = shift + " " + context.getString(R.string.sec_past);
            } else if (shift < 3600) {
                message = shift / 60 + " " + context.getString(R.string.min_past);
            } else {
                message = shift / 3600 + " " + context.getString(R.string.hour_past);
            }
        } else {
            message = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(time));
        }
        return message;
    }
    public static String getGenitiveMonth(Context context, String month) {
        if (context == null) {
            Log.w(TAG, "getGenitiveMonth | context is null");
            return month;
        }
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
    public static String getGenitiveMonth(Context context, int month) {
        String m = "";
        if (context == null) {
            Log.w(TAG, "getGenitiveMonth | context is null");
            return m;
        }
        switch (month) {
            case Calendar.JANUARY: m = context.getString(R.string.january_genitive); break;
            case Calendar.FEBRUARY: m = context.getString(R.string.february_genitive); break;
            case Calendar.MARCH: m = context.getString(R.string.march_genitive); break;
            case Calendar.APRIL: m = context.getString(R.string.april_genitive); break;
            case Calendar.MAY: m = context.getString(R.string.may_genitive); break;
            case Calendar.JUNE: m = context.getString(R.string.june_genitive); break;
            case Calendar.JULY: m = context.getString(R.string.july_genitive); break;
            case Calendar.AUGUST: m = context.getString(R.string.august_genitive); break;
            case Calendar.SEPTEMBER: m = context.getString(R.string.september_genitive); break;
            case Calendar.OCTOBER: m = context.getString(R.string.october_genitive); break;
            case Calendar.NOVEMBER: m = context.getString(R.string.november_genitive); break;
            case Calendar.DECEMBER: m = context.getString(R.string.december_genitive); break;
        }
        return m;
    }
    public static String getDay(Context context, int day) {
        String ret = "";
        switch (day) {
            case Calendar.MONDAY: ret = context.getString(R.string.monday); break;
            case Calendar.TUESDAY: ret = context.getString(R.string.tuesday); break;
            case Calendar.WEDNESDAY: ret = context.getString(R.string.wednesday); break;
            case Calendar.THURSDAY: ret = context.getString(R.string.thursday); break;
            case Calendar.FRIDAY: ret = context.getString(R.string.friday); break;
            case Calendar.SATURDAY: ret = context.getString(R.string.saturday); break;
            case Calendar.SUNDAY: ret = context.getString(R.string.sunday); break;
        }
        return ret;
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
    public static void toast(final Context context, @StringRes final int resId) {
        toast(context, context.getString(resId));
    }
    public static void toast(final Context context, final String text) {
        Static.T.runOnUiThread(() -> {
            if (context == null) {
                Log.w(TAG, "toast | context is null");
                return;
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        });
    }
    public static void snackBar(Activity activity, String text) {
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(android.R.id.content), text);
    }
    public static void snackBar(Activity activity, @IdRes int layout, String text) {
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(layout), text);
    }
    public static void snackBar(View layout, String text) {
        snackBar(layout, text, null, null);
    }
    public static void snackBar(Activity activity, String text, String action, View.OnClickListener onClickListener) {
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(android.R.id.content), text, action, onClickListener);
    }
    public static void snackBar(Activity activity, @IdRes int layout, String text, String action, View.OnClickListener onClickListener) {
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(layout), text, action, onClickListener);
    }
    public static void snackBar(final View layout, final String text, final String action, final View.OnClickListener onClickListener) {
        Static.T.runOnUiThread(() -> {
            if (layout != null) {
                Snackbar snackbar = Snackbar.make(layout, text, Snackbar.LENGTH_LONG);
                snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
                if (action != null) snackbar.setAction(action, onClickListener);
                snackbar.show();
            }
        });
    }
    public static void protocolChangesTrackSetup(final Context context, final int attempt) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            Log.v(TAG, "protocolChangesTrackSetup | attempt=" + attempt);
            if (!Storage.pref.get(context, "pref_protocol_changes_track", true)) {
                Log.v(TAG, "protocolChangesTrackSetup | pref_protocol_changes_track=false");
                return;
            }
            if (attempt < 3) {
                DeIfmoRestClient.get(context, "eregisterlog?days=126", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, Client.Headers headers, JSONObject responseObj, final JSONArray responseArr) {
                        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                            if (statusCode == 200 && responseArr != null) {
                                new ProtocolConverter(context, responseArr, 18, json -> Log.i(TAG, "protocolChangesTrackSetup | uploaded")).run();
                            } else {
                                protocolChangesTrackSetup(context, attempt + 1);
                            }
                        });
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> protocolChangesTrackSetup(context, attempt + 1));
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {}
                });
            }
        });
    }
    public static String getUserAgent(Context context) {
        try {
            if (Static.USER_AGENT == null) {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                Static.USER_AGENT = Static.USER_AGENT_TEMPLATE
                        .replace("{versionName}", pInfo.versionName)
                        .replace("{versionCode}", String.valueOf(pInfo.versionCode))
                        .replace("{sdkInt}", String.valueOf(Build.VERSION.SDK_INT));
            }
            return Static.USER_AGENT;
        } catch (Exception e) {
            return Static.USER_AGENT_TEMPLATE
                    .replace("{versionName}", "-")
                    .replace("{versionCode}", "-")
                    .replace("{sdkInt}", "-");
        }
    }
    public static class Translit  {
        public static String cyr2lat(char ch) {
            switch (ch) {
                case 'А': return "A";   case 'а': return "a";
                case 'Б': return "B";   case 'б': return "b";
                case 'В': return "V";   case 'в': return "v";
                case 'Г': return "G";   case 'г': return "g";
                case 'Д': return "D";   case 'д': return "d";
                case 'Е': return "E";   case 'е': return "e";
                case 'Ё': return "E";   case 'ё': return "e";
                case 'Ж': return "ZH";  case 'ж': return "zh";
                case 'З': return "Z";   case 'з': return "z";
                case 'И': return "I";   case 'и': return "i";
                case 'Й': return "J";   case 'й': return "j";
                case 'К': return "K";   case 'к': return "k";
                case 'Л': return "L";   case 'л': return "l";
                case 'М': return "M";   case 'м': return "m";
                case 'Н': return "N";   case 'н': return "n";
                case 'О': return "O";   case 'о': return "o";
                case 'П': return "P";   case 'п': return "p";
                case 'Р': return "R";   case 'р': return "r";
                case 'С': return "S";   case 'с': return "s";
                case 'Т': return "T";   case 'т': return "t";
                case 'У': return "U";   case 'у': return "u";
                case 'Ф': return "F";   case 'ф': return "f";
                case 'Х': return "KH";  case 'х': return "kh";
                case 'Ц': return "C";   case 'ц': return "c";
                case 'Ч': return "CH";  case 'ч': return "ch";
                case 'Ш': return "SH";  case 'ш': return "sh";
                case 'Щ': return "JSH"; case 'щ': return "jsh";
                case 'Ъ': return "''";  case 'ъ': return "''";
                case 'Ы': return "Y";   case 'ы': return "y";
                case 'Ь': return "'";   case 'ь': return "'";
                case 'Э': return "E";   case 'э': return "e";
                case 'Ю': return "JU";  case 'ю': return "ju";
                case 'Я': return "JA";  case 'я': return "ja";
                default: return String.valueOf(ch);
            }
        }
        public static String cyr2lat(String s) {
            StringBuilder sb = new StringBuilder(s.length() * 2);
            for (char ch: s.toCharArray()) {
                sb.append(cyr2lat(ch));
            }
            return sb.toString();
        }
    }
    public static class NavigationMenu {
        public static void displayEnableDisableOfflineButton(final NavigationView navigationView) {
            Static.T.runOnUiThread(() -> {
                if (navigationView != null) {
                    try {
                        Menu menu = navigationView.getMenu();
                        MenuItem nav_enable_offline_mode = menu.findItem(R.id.nav_enable_offline_mode);
                        MenuItem nav_disable_offline_mode = menu.findItem(R.id.nav_disable_offline_mode);
                        if (Static.OFFLINE_MODE) {
                            nav_enable_offline_mode.setVisible(false);
                            nav_disable_offline_mode.setVisible(true);
                        } else {
                            nav_enable_offline_mode.setVisible(true);
                            nav_disable_offline_mode.setVisible(false);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                }
            });
        }
        public static void displayUserData(final Context context, final NavigationView navigationView) {
            Static.T.runThread(() -> {
                final String name = Storage.file.perm.get(context, "user#name");
                final String group = Storage.file.perm.get(context, "user#groups");
                Static.T.runOnUiThread(() -> {
                    displayUserData(navigationView, R.id.user_name, name);
                    displayUserData(navigationView, R.id.user_group, group);
                });
            });
        }
        public static void displayUserData(final NavigationView navigationView, final int id, final String text) {
            Static.T.runOnUiThread(() -> {
                if (navigationView == null) return;
                View activity_main_nav_header = navigationView.getHeaderView(0);
                if (activity_main_nav_header == null) return;
                TextView textView = activity_main_nav_header.findViewById(id);
                if (textView != null) {
                    if (!text.isEmpty()) {
                        textView.setText(text);
                        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    } else {
                        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                    }
                }
            });
        }
        public static void snackbarOffline(final Activity activity) {
            Static.T.runOnUiThread(() -> {
                if (Static.OFFLINE_MODE) {
                    Static.snackBar(activity, activity.getString(R.string.offline_mode_on));
                }
            });
        }
        public static void drawOffline(final Menu menu) {
            Static.T.runOnUiThread(() -> {
                if (menu != null) {
                    if (Static.OFFLINE_MODE) {
                        for (int i = 0; i < menu.size(); i++) {
                            menu.getItem(i).setVisible(false);
                        }
                    }
                    MenuItem menuItem = menu.findItem(R.id.offline_mode);
                    if (menuItem != null) {
                        menuItem.setVisible(Static.OFFLINE_MODE);
                    }
                }
            });
        }
        public static void displayRemoteMessage(final Activity activity) {
            Static.T.runThread(() -> FirebaseConfigProvider.getJson(FirebaseConfigProvider.MESSAGE_MENU, value -> Static.T.runThread(() -> {
                try {
                    if (value == null) return;
                    final int type = value.getInt("type");
                    final String message = value.getString("message");
                    if (message == null || message.trim().isEmpty()) return;
                    final String hash = Static.crypt(message);
                    if (hash != null && hash.equals(Storage.file.general.perm.get(activity, "firebase#remote_message#menu", ""))) {
                        return;
                    }
                    Static.T.runOnUiThread(() -> {
                        final ViewGroup message_menu = activity.findViewById(R.id.message_menu);
                        final View message_menu_separator = activity.findViewById(R.id.message_menu_separator);
                        final View layout = Static.getRemoteMessage(activity, type, message, (context, view) -> {
                            if (hash != null) {
                                if (Storage.file.general.perm.put(activity, "firebase#remote_message#menu", hash)) {
                                    if (message_menu != null && view != null) {
                                        message_menu.removeView(view);
                                        if (message_menu_separator != null) {
                                            message_menu_separator.setVisibility(View.GONE);
                                        }
                                    }
                                    Static.snackBar(activity, activity.getString(R.string.notification_dismissed), activity.getString(R.string.undo), v -> Static.T.runThread(() -> {
                                        if (Storage.file.general.perm.delete(activity, "firebase#remote_message#menu")) {
                                            Static.T.runOnUiThread(() -> {
                                                if (message_menu != null && view != null) {
                                                    message_menu.addView(view);
                                                    if (message_menu_separator != null) {
                                                        message_menu_separator.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            });
                                        }
                                    }));
                                }
                            }
                        });
                        if (layout != null && message_menu != null) {
                            message_menu.removeAllViews();
                            message_menu.addView(layout);
                            if (message_menu_separator != null) {
                                message_menu_separator.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } catch (Exception ignore) {
                    // ignore
                }
            })));
        }
        public static void hideIfUnauthorizedMode(final NavigationView navigationView) {
            Static.T.runOnUiThread(() -> {
                try {
                    if (navigationView != null) {
                        final Menu menu = navigationView.getMenu();
                        if (menu.findItem(R.id.nav_e_register).isVisible()) {
                            if (Static.UNAUTHORIZED_MODE) {
                                menu.findItem(R.id.nav_e_register).setVisible(false);
                                menu.findItem(R.id.nav_protocol_changes).setVisible(false);
                                menu.findItem(R.id.nav_room101).setVisible(false);
                                menu.findItem(R.id.nav_do_clean_auth).setVisible(false);
                                menu.findItem(R.id.nav_logout).setVisible(false);
                            }
                        } else {
                            if (!Static.UNAUTHORIZED_MODE) {
                                menu.findItem(R.id.nav_e_register).setVisible(true);
                                menu.findItem(R.id.nav_protocol_changes).setVisible(true);
                                menu.findItem(R.id.nav_room101).setVisible(true);
                                menu.findItem(R.id.nav_do_clean_auth).setVisible(true);
                                menu.findItem(R.id.nav_logout).setVisible(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            });
        }
    }
    @SuppressWarnings("deprecation")
    public static Locale getLocale(Context context) {
        Locale locale;
        String lang = Storage.pref.get(context, "pref_lang", "default");
        switch (lang) {
            case "ru": case "en": {
                locale = new Locale(lang);
                break;
            }
            default: case "default": {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = new Locale(context.getResources().getConfiguration().getLocales().get(0).getCountry());
                } else {
                    locale = new Locale(context.getResources().getConfiguration().locale.getCountry());
                }
                break;
            }
        }
        return locale;
    }
    public static void removeView(final View view) {
        Static.T.runOnUiThread(() -> {
            try {
                ((ViewGroup) view.getParent()).removeView(view);
            } catch (Throwable e) {
                Static.error(e);
            }
        });
    }
    public static String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    @SuppressWarnings("deprecation")
    public static String escapeString(String text) {
        if (text == null) return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            return android.text.Html.fromHtml(text).toString().trim();
        }
    }
    public static String cuteDate(Context context, String date_format, String date_string) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context));
        Calendar date = getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .append(" ")
                .append(ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }
    public static String cuteDate(Context context, String date_format, String date_start, String date_end) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context));
        Calendar calendar_start = getCalendar();
        Calendar calendar_end = getCalendar();
        calendar_start.setTime(format_input.parse(date_start));
        calendar_end.setTime(format_input.parse(date_end));
        boolean diff_days = calendar_start.get(Calendar.DATE) != calendar_end.get(Calendar.DATE);
        boolean diff_months = calendar_start.get(Calendar.MONTH) != calendar_end.get(Calendar.MONTH);
        boolean diff_years = calendar_start.get(Calendar.YEAR) != calendar_end.get(Calendar.YEAR);
        StringBuilder sb = new StringBuilder();
        if (diff_days || diff_months || diff_years) {
            sb.append(calendar_start.get(Calendar.DATE));
        }
        if (diff_months || diff_years) {
            sb.append(" ").append(getGenitiveMonth(context, calendar_start.get(Calendar.MONTH)));
        }
        if (diff_years) {
            sb.append(" ").append(calendar_start.get(Calendar.YEAR));
        }
        if (diff_days || diff_months || diff_years) {
            sb.append(" - ");
        }
        sb.append(calendar_end.get(Calendar.DATE)).append(" ").append(getGenitiveMonth(context, calendar_end.get(Calendar.MONTH))).append(" ").append(calendar_end.get(Calendar.YEAR));
        return sb.toString();
    }
    public static String cuteDateWithoutTime(Context context, String date_format, String date_string) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context));
        Calendar date = getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(getGenitiveMonth(context, date.get(Calendar.MONTH)))
                .append(" ")
                .append(date.get(Calendar.YEAR))
                .toString();
    }
    @SuppressLint("DefaultLocale")
    public static String ldgZero(int number) {
        return String.format("%02d", number);
    }
    public static class ColorPicker {
        private static final String TAG = "Static.ColorPicker";
        private static final String[][] COLORS = {
                {"#F44336", "#FFEBEE", "#FFCDD2", "#EF9A9A", "#E57373", "#EF5350", "#F44336", "#E53935", "#D32F2F", "#C62828", "#B71C1C"}, /* Red */
                {"#E91E63", "#FCE4EC", "#F8BBD0", "#F48FB1", "#F06292", "#EC407A", "#E91E63", "#D81B60", "#C2185B", "#AD1457", "#880E4F"}, /* Pink */
                {"#9C27B0", "#F3E5F5", "#E1BEE7", "#CE93D8", "#BA68C8", "#AB47BC", "#9C27B0", "#8E24AA", "#7B1FA2", "#6A1B9A", "#4A148C"}, /* Purple */
                {"#673AB7", "#EDE7F6", "#D1C4E9", "#B39DDB", "#9575CD", "#7E57C2", "#673AB7", "#5E35B1", "#512DA8", "#4527A0", "#311B92"}, /* Deep Purple */
                {"#3F51B5", "#E8EAF6", "#C5CAE9", "#9FA8DA", "#7986CB", "#5C6BC0", "#3F51B5", "#3949AB", "#303F9F", "#283593", "#1A237E"}, /* Indigo */
                {"#2196F3", "#E3F2FD", "#BBDEFB", "#90CAF9", "#64B5F6", "#42A5F5", "#2196F3", "#1E88E5", "#1976D2", "#1565C0", "#0D47A1"}, /* Blue */
                {"#03A9F4", "#E1F5FE", "#B3E5FC", "#81D4FA", "#4FC3F7", "#29B6F6", "#03A9F4", "#039BE5", "#0288D1", "#0277BD", "#01579B"}, /* Light Blue */
                {"#00BCD4", "#E0F7FA", "#B2EBF2", "#80DEEA", "#4DD0E1", "#26C6DA", "#00BCD4", "#00ACC1", "#0097A7", "#00838F", "#006064"}, /* Cyan */
                {"#009688", "#E0F2F1", "#B2DFDB", "#80CBC4", "#4DB6AC", "#26A69A", "#009688", "#00897B", "#00796B", "#00695C", "#004D40"}, /* Teal */
                {"#4CAF50", "#E8F5E9", "#C8E6C9", "#A5D6A7", "#81C784", "#66BB6A", "#4CAF50", "#43A047", "#388E3C", "#2E7D32", "#1B5E20"}, /* Green */
                {"#8BC34A", "#F1F8E9", "#DCEDC8", "#C5E1A5", "#AED581", "#9CCC65", "#8BC34A", "#7CB342", "#689F38", "#558B2F", "#33691E"}, /* Light Green */
                {"#CDDC39", "#F9FBE7", "#F0F4C3", "#E6EE9C", "#DCE775", "#D4E157", "#CDDC39", "#C0CA33", "#AFB42B", "#9E9D24", "#827717"}, /* Lime */
                {"#FFEB3B", "#FFFDE7", "#FFF9C4", "#FFF59D", "#FFF176", "#FFEE58", "#FFEB3B", "#FDD835", "#FBC02D", "#F9A825", "#F57F17"}, /* Yellow */
                {"#FFC107", "#FFF8E1", "#FFECB3", "#FFE082", "#FFD54F", "#FFCA28", "#FFC107", "#FFB300", "#FFA000", "#FF8F00", "#FF6F00"}, /* Amber */
                {"#FF9800", "#FFF3E0", "#FFE0B2", "#FFCC80", "#FFB74D", "#FFA726", "#FF9800", "#FB8C00", "#F57C00", "#EF6C00", "#E65100"}, /* Orange */
                {"#FF5722", "#FBE9E7", "#FFCCBC", "#FFAB91", "#FF8A65", "#FF7043", "#FF5722", "#F4511E", "#E64A19", "#D84315", "#BF360C"}, /* Deep Orange */
                {"#795548", "#EFEBE9", "#D7CCC8", "#BCAAA4", "#A1887F", "#8D6E63", "#795548", "#6D4C41", "#5D4037", "#4E342E", "#3E2723"}, /* Brown */
                {"#9E9E9E", "#FAFAFA", "#F5F5F5", "#EEEEEE", "#E0E0E0", "#BDBDBD", "#9E9E9E", "#757575", "#616161", "#424242", "#212121"}, /* Grey */
                {"#607D8B", "#ECEFF1", "#CFD8DC", "#B0BEC5", "#90A4AE", "#78909C", "#607D8B", "#546E7A", "#455A64", "#37474F", "#263238"}, /* Blue Grey */
                {"#000000"}, /* Black */
                {"#FFFFFF"}  /* White */
        };
        public static class Instance {
            private final Context context;
            private final ColorPickerCallback callback;
            private AlertDialog alertDialog = null;
            private GridView container = null;
            private GridAdapter gridAdapter = null;
            private String selected = "";
            public Instance(final Context context, final ColorPickerCallback callback) {
                Log.v(TAG, "new Instance");
                this.context = context;
                this.callback = callback;
                Static.T.runOnUiThread(() -> {
                    try {
                        ViewGroup layout = (ViewGroup) inflate(context, R.layout.layout_color_picker_dialog);
                        container = layout.findViewById(R.id.colorPickerContainer);
                        alertDialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.choose_color)
                                .setView(layout)
                                .setPositiveButton(R.string.apply, (dialogInterface, i) -> {
                                    Log.v(TAG, "apply | selected=" + selected);
                                    if (!selected.isEmpty()) {
                                        callback.result(selected);
                                    }
                                })
                                .setNegativeButton(R.string.do_cancel, null)
                                .create();
                    } catch (Exception e) {
                        callback.exception(e);
                    }
                });
            }
            public void show() {
                Log.v(TAG, "show");
                Static.T.runOnUiThread(() -> {
                    try {
                        if (alertDialog != null && !alertDialog.isShowing()) {
                            alertDialog.show();
                            displayColors(-1);
                        }
                    } catch (Exception e) {
                        callback.exception(e);
                    }
                });
            }
            public void close() {
                Log.v(TAG, "close");
                Static.T.runOnUiThread(() -> {
                    try {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                    } catch (Exception e) {
                        callback.exception(e);
                    }
                });
            }
            private void displayColors(final int index) {
                Log.v(TAG, "displayColors | index=" + index);
                Static.T.runThread(() -> {
                    try {
                        final boolean modeAllColors = index < 0 || index > COLORS.length;
                        final String[] colors;
                        if (modeAllColors) { // display all colors
                            colors = new String[COLORS.length];
                            for (int i = 0; i < COLORS.length; i++) {
                                colors[i] = COLORS[i][0];
                            }
                        } else { // display certain colors
                            colors = new String[COLORS[index].length];
                            System.arraycopy(COLORS[index], 1, colors, 0, COLORS[index].length - 1);
                            colors[COLORS[index].length - 1] = "back";
                        }
                        Static.T.runOnUiThread(() -> {
                            try {
                                if (gridAdapter == null) {
                                    gridAdapter = new GridAdapter(context);
                                    container.setAdapter(gridAdapter);
                                }
                                container.setOnItemClickListener((adapterView, view, i, l) -> {
                                    Log.v(TAG, "color clicked | i=" + i);
                                    Static.T.runThread(() -> {
                                        try {
                                            if (modeAllColors) {
                                                if (COLORS[i].length > 1) {
                                                    displayColors(i);
                                                } else {
                                                    String color = gridAdapter.getItem(i);
                                                    if ("back".equals(color)) {
                                                        Log.v(TAG, "back clicked");
                                                        displayColors(-1);
                                                    } else {
                                                        Log.v(TAG, "color selected | color=" + color);
                                                        selected = color;
                                                        Static.T.runOnUiThread(() -> gridAdapter.notifyDataSetChanged());
                                                    }
                                                }
                                            } else {
                                                String color = gridAdapter.getItem(i);
                                                if ("back".equals(color)) {
                                                    Log.v(TAG, "back clicked");
                                                    displayColors(-1);
                                                } else {
                                                    Log.v(TAG, "color selected | color=" + color);
                                                    selected = color;
                                                    Static.T.runOnUiThread(() -> gridAdapter.notifyDataSetChanged());
                                                }
                                            }
                                        } catch (Exception e) {
                                            callback.exception(e);
                                        }
                                    });
                                });
                                gridAdapter.updateColors(colors);
                                gridAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                callback.exception(e);
                            }
                        });
                    } catch (Exception e) {
                        callback.exception(e);
                    }
                });
            }
            private class GridAdapter extends BaseAdapter {
                private final Context context;
                private String[] colors;
                private GridAdapter(Context context) {
                    this.context = context;
                    this.colors = new String[0];
                }
                private void updateColors(String[] colors) {
                    this.colors = colors;
                }
                @Override
                public int getCount() {
                    return colors.length;
                }
                @Override
                public String getItem(int position) {
                    return colors[position];
                }
                @Override
                public long getItemId(int position) {
                    return position;
                }
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    try {
                        if (convertView == null) {
                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            convertView = inflater.inflate(R.layout.layout_color_picker_dialog_item, parent, false);
                        }
                        String color = getItem(position);
                        if ("back".equals(color)) {
                            GradientDrawable sd = (GradientDrawable) convertView.getBackground();
                            sd.setColor(Color.BLACK);
                            convertView.findViewById(R.id.sign_selected).setVisibility(View.GONE);
                            convertView.findViewById(R.id.sign_back).setVisibility(View.VISIBLE);
                            ImageView sign_selected_mark = convertView.findViewById(R.id.sign_back_mark);
                            sign_selected_mark.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                        } else {
                            GradientDrawable sd = (GradientDrawable) convertView.getBackground();
                            sd.setColor(Color.parseColor(color));
                            if (color.toLowerCase().equals(selected.toLowerCase())) {
                                convertView.findViewById(R.id.sign_selected).setVisibility(View.VISIBLE);
                                ImageView sign_selected_mark = convertView.findViewById(R.id.sign_selected_mark);
                                sign_selected_mark.setImageTintList(ColorStateList.valueOf(Color.parseColor(color) > Color.parseColor("#757575") ? Color.BLACK : Color.WHITE));
                            } else {
                                convertView.findViewById(R.id.sign_selected).setVisibility(View.GONE);
                            }
                            convertView.findViewById(R.id.sign_back).setVisibility(View.GONE);
                        }
                        return convertView;
                    } catch (Exception e) {
                        callback.exception(e);
                        return null;
                    }
                }
            }
        }
        public static Instance get(final Context context, final ColorPickerCallback callback) {
            return new Instance(context, callback);
        }
        public interface ColorPickerCallback {
            void result(String hex);
            void exception(Exception e);
        }
        private static View inflate(Context context, int layoutId) throws InflateException {
            return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
        }
    }
    public interface RemoteMessageCallback {
        void onDismiss(Context context, View view);
    }
    public static View getRemoteMessage(final Context context, final int type, final String message, final RemoteMessageCallback callback) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return null;
            }
            int layoutId;
            switch (type) {
                case 0:
                default: {
                    layoutId = R.layout.layout_remote_message_info;
                    break;
                }
                case 1: {
                    layoutId = R.layout.layout_remote_message_warn;
                    break;
                }
            }
            final View layout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
            ((TextView) layout.findViewById(R.id.text)).setText(message);
            layout.setOnClickListener(v -> callback.onDismiss(context, layout));
            return layout;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    public static String getAppTheme(final Context context) {
        if (app_theme == null) {
            updateAppTheme(context);
        }
        return app_theme;
    }
    public static void updateAppTheme(final Context context) {
        app_theme = ThemeUtil.getTheme(context);
    }
    public static void applyActivityTheme(final Activity activity) {
        if (activity != null) {
            switch (Static.getAppTheme(activity)) {
                case "light":
                default: activity.setTheme(R.style.AppTheme); break;
                case "dark": activity.setTheme(R.style.AppTheme_Dark); break;
                case "white": activity.setTheme(R.style.AppTheme_White); break;
                case "black": activity.setTheme(R.style.AppTheme_Black); break;
            }
        }
    }
    public static void applyToolbarTheme(final Context context, final Toolbar toolbar) {
        if (toolbar != null) {
            Context toolbar_context = toolbar.getContext();
            if (toolbar_context != null) {
                switch (Static.getAppTheme(context)) {
                    case "light":
                    default: toolbar_context.setTheme(R.style.AppTheme_Toolbar); break;
                    case "dark": toolbar_context.setTheme(R.style.AppTheme_Toolbar_Dark); break;
                    case "white": toolbar_context.setTheme(R.style.AppTheme_Toolbar_White); break;
                    case "black": toolbar_context.setTheme(R.style.AppTheme_Toolbar_Black); break;
                }
            }
        }
    }
    public static JSONArray string2jsonArray(String text) throws JSONException {
        JSONArray json;
        if (text == null || text.isEmpty()) {
            json = new JSONArray();
        } else {
            json = new JSONArray(text);
        }
        return json;
    }
    public static JSONObject string2json(String text) throws JSONException {
        JSONObject json;
        if (text == null || text.isEmpty()) {
            json = new JSONObject();
        } else {
            json = new JSONObject(text);
        }
        return json;
    }
    public static String prettifyGroupNumber(String group) {
        if (group != null && !group.isEmpty()) {
            Matcher m;
            m = Pattern.compile("(.*)([a-zа-яё])(\\d{4}[a-zа-яё]?)(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) + m.group(2).toUpperCase() + m.group(3).toLowerCase() + m.group(4);
            }
            m = Pattern.compile("(.*)([a-zа-яё]{2}\\d{2}[a-zа-яё]{1,3})(.*)", Pattern.CASE_INSENSITIVE).matcher(group);
            if (m.find()) {
                group = m.group(1) + m.group(2).toUpperCase() + m.group(3);
            }
        }
        return group;
    }
}
