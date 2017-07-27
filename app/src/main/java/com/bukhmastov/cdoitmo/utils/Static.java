package com.bukhmastov.cdoitmo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

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

public class Static {

    private static final String TAG = "Static";
    public static String versionName;
    public static int versionCode;
    public static int textColorPrimary, textColorSecondary, colorSeparator, colorBackgroundSnackBar, colorAccent, colorBackgroundRefresh;
    public static float destiny;
    public static TypedValue typedValue;
    public static boolean OFFLINE_MODE = false;
    public static boolean firstLaunch = true;
    public static boolean authorized = false;
    public static ProtocolTracker protocolTracker = null;
    public static boolean darkTheme = false;
    public static int intentFlagRestart = 268468224;
    public static boolean tablet = false;
    private static final String USER_AGENT_TEMPLATE = "CDOITMO/{versionName}/{versionCode} Java/Android/{sdkInt}";
    private static String USER_AGENT = null;

    public static class T {
        private static final String TAG = "Static.T";
        private static final boolean DEBUG = false;
        public enum TYPE {FOREGROUND, BACKGROUND}
        private static class Th {
            public TYPE type;
            public HandlerThread thread = null;
            public String thread_name;
            public int thread_priority;
            public Th(TYPE type, String thread_name, int thread_priority) {
                this.type = type;
                this.thread_name = thread_name;
                this.thread_priority = thread_priority;
            }
        }
        private static Th Foreground = new Th(TYPE.FOREGROUND, "CDOExecutorForeground", Process.THREAD_PRIORITY_FOREGROUND);
        private static Th Background = new Th(TYPE.BACKGROUND, "CDOExecutorBackground", Process.THREAD_PRIORITY_BACKGROUND);

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
                new Handler(th.thread.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            runnable.run();
                        } catch (Throwable throwable) {
                            Log.exception("Run on " + th.thread.getName() + " thread failed", throwable);
                        }
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
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                runnable.run();
                            } catch (Throwable throwable) {
                                Log.exception("Run on main thread failed", throwable);
                            }
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
            typedValue = new TypedValue();
            textColorPrimary = resolveColor(activity, android.R.attr.textColorPrimary);
            textColorSecondary = resolveColor(activity, android.R.attr.textColorSecondary);
            colorSeparator = resolveColor(activity, R.attr.colorSeparator);
            colorBackgroundSnackBar = resolveColor(activity, R.attr.colorBackgroundSnackBar);
            colorAccent = resolveColor(activity, R.attr.colorAccent);
            colorBackgroundRefresh = resolveColor(activity, R.attr.colorBackgroundRefresh);
            destiny = activity.getResources().getDisplayMetrics().density;
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public static void error(Throwable throwable){
        Log.exception(throwable);
    }
    public static boolean isOnline(Context context) {
        Log.v(TAG, "isOnline");
        if (context != null) {
            NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return true;
        }
    }
    public static void reLaunch(Context context){
        Log.i(TAG, "reLaunch");
        if (context == null) {
            Log.w(TAG, "reLaunch | context is null");
            return;
        }
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Static.intentFlagRestart);
        context.startActivity(intent);
    }
    public static void hardReset(Context context){
        Log.i(TAG, "hardReset");
        if (context == null) {
            Log.w(TAG, "hardReset | context is null");
            return;
        }
        Static.logout(context);
        Storage.file.all.reset(context);
        Storage.pref.put(context, "pref_open_drawer_at_startup", true);
        Static.firstLaunch = true;
        Static.OFFLINE_MODE = false;
        MainActivity.loaded = false;
        Static.reLaunch(context);
    }
    public static int resolveColor(Context context, int reference) throws Exception {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(reference, typedValue, true);
        return context.obtainStyledAttributes(typedValue.data, new int[]{reference}).getColor(0, -1);
    }
    public static int getWeek(Context context){
        try {
            String weekStr = Storage.file.general.get(context, "user#week").trim();
            if (!weekStr.isEmpty()) {
                JSONObject jsonObject = new JSONObject(weekStr);
                int week = jsonObject.getInt("week");
                if (week >= 0) {
                    Calendar past = Calendar.getInstance();
                    past.setTimeInMillis(jsonObject.getLong("timestamp"));
                    return week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                }
            }
        } catch (JSONException e) {
            Storage.file.general.delete(context, "user#week");
        }
        return -1;
    }
    public static void logout(Context context){
        Log.i(TAG, "logout");
        new ProtocolTracker(context).stop();
        Storage.file.all.clear(context);
        Static.logoutCurrent(context);
        Static.authorized = false;
    }
    public static void logoutCurrent(Context context){
        Log.i(TAG, "logoutCurrent");
        new ProtocolTracker(context).stop();
        Storage.file.general.delete(context, "users#current_login");
    }
    public static void lockOrientation(Activity activity, boolean lock){
        Log.v(TAG, "lockOrientation | lock=" + (lock ? "true" : "false"));
        if (activity != null) {
            activity.setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }
    public static void showUpdateTime(Activity activity, long time, boolean show_now){
        showUpdateTime(activity, android.R.id.content, time, show_now);
    }
    public static void showUpdateTime(Activity activity, @IdRes int layout, long time, boolean show_now){
        String message = getUpdateTime(activity, time);
        int shift = (int) ((Calendar.getInstance().getTimeInMillis() - time) / 1000L);
        if (show_now || shift > 4) {
            Static.snackBar(activity, layout, activity.getString(R.string.update_date) + " " + message);
        }
    }
    public static String getUpdateTime(Activity activity, long time) {
        int shift = (int) ((Calendar.getInstance().getTimeInMillis() - time) / 1000L);
        String message;
        if (shift < 21600 && activity != null) {
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
    public static void toast(final Context context, final String text){
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context == null) {
                    Log.w(TAG, "toast | context is null");
                    return;
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void snackBar(Activity activity, String text){
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(android.R.id.content), text);
    }
    public static void snackBar(Activity activity, @IdRes int layout, String text){
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(layout), text);
    }
    public static void snackBar(View layout, String text){
        snackBar(layout, text, null, null);
    }
    public static void snackBar(Activity activity, String text, String action, View.OnClickListener onClickListener){
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(android.R.id.content), text, action, onClickListener);
    }
    public static void snackBar(Activity activity, @IdRes int layout, String text, String action, View.OnClickListener onClickListener){
        if (activity == null) {
            Log.w(TAG, "snackBar | activity is null");
            return;
        }
        Static.snackBar(activity.findViewById(layout), text, action, onClickListener);
    }
    public static void snackBar(final View layout, final String text, final String action, final View.OnClickListener onClickListener){
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (layout != null) {
                    Snackbar snackbar = Snackbar.make(layout, text, Snackbar.LENGTH_LONG);
                    snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
                    if (action != null) snackbar.setAction(action, onClickListener);
                    snackbar.show();
                }
            }
        });
    }
    public static void protocolChangesTrackSetup(final Context context, final int attempt){
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "protocolChangesTrackSetup | attempt=" + attempt);
                if (!Storage.pref.get(context, "pref_protocol_changes_track", true)) {
                    Log.v(TAG, "protocolChangesTrackSetup | pref_protocol_changes_track=false");
                    return;
                }
                if (attempt < 3) {
                    DeIfmoRestClient.get(context, "eregisterlog?days=126", null, new DeIfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final JSONObject responseObj, final JSONArray responseArr) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    if (statusCode == 200 && responseArr != null) {
                                        try {
                                            JSONArray array = new JSONArray();
                                            array.put(18);
                                            new ProtocolConverter(context, new ProtocolConverter.response() {
                                                @Override
                                                public void finish(JSONObject json) {
                                                    Log.i(TAG, "protocolChangesTrackSetup | uploaded");
                                                }
                                            }).execute(responseArr, array);
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    } else {
                                        protocolChangesTrackSetup(context, attempt + 1);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onFailure(int statusCode, int state) {}
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {}
                    });
                }
            }
        });
    }
    public static String getUserAgent(Context context){
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
    public static String getSafetyRequestParams(RequestParams params){
        return (params == null ? "null" : (params.has("password") ? "<HIDDEN due to presence of the password>" : params.toString()));
    }
    public static class Translit {
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
        private static RequestHandle navRequestHandle = null;
        public static void displayEnableDisableOfflineButton(final NavigationView navigationView) {
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
        public static void displayUserData(final Context context, final NavigationView navigationView) {
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayUserData(navigationView, R.id.user_name, Storage.file.perm.get(context, "user#name"));
                    displayUserData(navigationView, R.id.user_group, Storage.file.perm.get(context, "user#group"));
                }
            });
        }
        public static void displayUserData(final NavigationView navigationView, final int id, final String text) {
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (navigationView == null) return;
                    View activity_main_nav_header = navigationView.getHeaderView(0);
                    if (activity_main_nav_header == null) return;
                    TextView textView = (TextView) activity_main_nav_header.findViewById(id);
                    if (textView != null) {
                        if (!text.isEmpty()) {
                            textView.setText(text);
                            textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        } else {
                            textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                        }
                    }
                }
            });
        }
        public static void displayUserAvatar(final Context context, final NavigationView navigationView) {
            /*Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    if (navigationView == null) return;
                    String url = Storage.file.perm.get(context, "user#avatar").trim();
                    if (!url.isEmpty() && !url.contains("distributedCDE?Rule=GETATTACH&ATT_ID=1941771")) {
                        if (navRequestHandle != null) {
                            navRequestHandle.cancel(true);
                            navRequestHandle = null;
                        }
                        DeIfmoClient.getAvatar(context, url, new DeIfmoDrawableClientResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final Bitmap bitmap) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final Drawable drawable = new BitmapDrawable(context.getResources(), getCroppedBitmap(bitmap));
                                            Static.T.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        ViewGroup frameLayout = (ViewGroup) navigationView.findViewById(R.id.user_icon);
                                                        if (frameLayout != null) {
                                                            ImageView imageView = (ImageView) frameLayout.getChildAt(0);
                                                            if (imageView != null) {
                                                                imageView.setImageDrawable(drawable);
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        Static.error(e);
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            Static.error(e);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onProgress(int state) {}
                            @Override
                            public void onFailure(int statusCode, int state) {}
                            @Override
                            public void onNewHandle(RequestHandle requestHandle) {
                                navRequestHandle = requestHandle;
                            }
                        });
                    } else {
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    FrameLayout frameLayout = (FrameLayout) navigationView.findViewById(R.id.user_icon);
                                    if (frameLayout != null) {
                                        ImageView imageView = (ImageView) frameLayout.getChildAt(0);
                                        if (imageView != null) {
                                            TypedArray a = context.getTheme().obtainStyledAttributes(R.style.AppTheme, new int[] {R.attr.ic_cdo_small});
                                            Drawable drawable = context.getResources().getDrawable(a.getResourceId(0, 0), context.getTheme());
                                            a.recycle();
                                            imageView.setImageDrawable(drawable);
                                        }
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                }
                            }
                        });
                    }
                }
            });*/
        }
        private static Bitmap getCroppedBitmap(Bitmap bitmap) {
            int dimen = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap output = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Rect rect = new Rect(0, 0, dimen, dimen);
            final Paint paint = new Paint();
            canvas.drawARGB(0, 0, 0, 0);
            paint.setAntiAlias(true);
            paint.setColor(Color.parseColor("#ffffff"));
            canvas.drawCircle(dimen / 2, dimen / 2, dimen / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
        }
        public static void snackbarOffline(final Activity activity) {
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Static.OFFLINE_MODE) {
                        Static.snackBar(activity, activity.getString(R.string.offline_mode_on));
                    }
                }
            });
        }
        public static void drawOffline(final Menu menu) {
            Static.T.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
    }
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
    public static boolean removeView(View view) {
        try {
            ((ViewGroup) view.getParent()).removeView(view);
        } catch (Throwable e) {
            Static.error(e);
            return false;
        }
        return true;
    }
    public static String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    public static String escapeString(String text) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            return android.text.Html.fromHtml(text).toString().trim();
        }
    }
    public static String cuteDate(Context context, String date_format, String date_string) throws ParseException {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, getLocale(context));
        Calendar date = Calendar.getInstance();
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
        Calendar calendar_start = Calendar.getInstance();
        Calendar calendar_end = Calendar.getInstance();
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
    @SuppressLint("DefaultLocale")
    public static String ldgZero(int number) {
        return String.format("%02d", number);
    }

}
