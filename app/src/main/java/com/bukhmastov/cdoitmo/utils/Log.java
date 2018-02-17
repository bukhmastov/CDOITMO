package com.bukhmastov.cdoitmo.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Log {

    public static String TAGD = "TAG_DEBUG";
    private static boolean enabled = false;
    public static void setEnabled(boolean enabled) {
        Log.enabled = enabled;
        if (!Log.enabled && logList != null) {
            logList.clear();
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.getDefault());
    private static ArrayList<LogItem> logList = null;
    private static class LogItem {
        public final int type;
        public final long ts;
        public String TAG;
        public String log;
        public Throwable throwable;
        public LogItem(int type, String TAG, String log){
            this.type = type;
            this.ts = System.currentTimeMillis();
            this.TAG = TAG;
            this.log = log;
        }
        public LogItem(int type, Throwable throwable){
            this.type = type;
            this.ts = System.currentTimeMillis();
            this.throwable = throwable;
        }
    }

    private static final int VERBOSE = android.util.Log.VERBOSE;
    private static final int DEBUG = android.util.Log.DEBUG;
    private static final int INFO = android.util.Log.INFO;
    private static final int WARN = android.util.Log.WARN;
    private static final int ERROR = android.util.Log.ERROR;
    private static final int EXCEPTION = 7331;
    private static final int WTF = android.util.Log.ASSERT;
    private static final int WTF_EXCEPTION = 8442;
    public static class Metrics {
        public static int warn = 0;
        public static int error = 0;
        public static int exception = 0;
        public static int wtf = 0;
    }

    @NonNull
    public static String getLog() {
        return getLog(true);
    }
    @NonNull
    public static String getLog(boolean reverse) {
        if (logList == null) {
            logList = new ArrayList<>();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-------Device--------").append("\n");
        stringBuilder.append("DEVICE: ").append(Build.DEVICE).append("\n");
        stringBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
        stringBuilder.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
        stringBuilder.append("DISPLAY: ").append(Build.DISPLAY).append("\n");
        stringBuilder.append("SDK_INT: ").append(Build.VERSION.SDK_INT).append("\n");
        stringBuilder.append("-----Application-----").append("\n");
        stringBuilder.append(Static.versionName).append(" (").append(Static.versionCode).append(")").append("\n");
        stringBuilder.append("---------Log---------").append("\n");
        for (int i = reverse ? (logList.size() - 1) : 0; reverse ? (i >= 0) : (i < logList.size()); i = reverse ? (i - 1) : (i + 1)) {
            LogItem logItem = logList.get(i);
            if (logItem == null) continue;
            stringBuilder.append(dateFormat.format(logItem.ts)).append(" | ");
            switch (logItem.type) {
                case VERBOSE: stringBuilder.append("VERBOSE").append("/"); break;
                case DEBUG: stringBuilder.append("DEBUG").append("/"); break;
                case INFO: stringBuilder.append("INFO").append("/"); break;
                case WARN: stringBuilder.append("WARN").append("/"); break;
                case ERROR: stringBuilder.append("ERROR").append("/"); break;
                case EXCEPTION: stringBuilder.append("EXCEPTION").append(": "); break;
                case WTF: stringBuilder.append("WTF").append("/"); break;
                case WTF_EXCEPTION: stringBuilder.append("WTF_EXCEPTION").append(": "); break;
            }
            if ((logItem.type == EXCEPTION || logItem.type == WTF_EXCEPTION) && logItem.throwable != null) {
                stringBuilder.append(logItem.throwable.getMessage());
                StackTraceElement[] stackTrace = logItem.throwable.getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    stringBuilder.append("\n").append("    ").append("at").append(" ").append(element.toString());
                }
            } else {
                stringBuilder.append(logItem.TAG).append(" ").append(logItem.log);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public static int v(String TAG, Object... log) {
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.v(TAG, l);
        if (enabled) {
            addLog(new LogItem(VERBOSE, TAG, l));
            return android.util.Log.v(TAG, l);
        } else {
            return 0;
        }
    }
    public static int d(Object... log) {
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.d(TAGD, l);
        if (enabled) {
            addLog(new LogItem(DEBUG, TAGD, l));
            return android.util.Log.d(TAGD, l);
        } else {
            return 0;
        }
    }
    public static int i(String TAG, Object... log) {
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.i(TAG, l);
        if (enabled) {
            addLog(new LogItem(INFO, TAG, l));
            return android.util.Log.i(TAG, l);
        } else {
            return 0;
        }
    }
    public static int w(String TAG, Object... log) {
        Metrics.warn++;
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.w(TAG, l);
        if (enabled) {
            addLog(new LogItem(WARN, TAG, l));
            return android.util.Log.w(TAG, l);
        } else {
            return 0;
        }
    }
    public static int e(String TAG, Object... log) {
        Metrics.error++;
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.e(TAG, l);
        if (enabled) {
            addLog(new LogItem(ERROR, TAG, l));
            return android.util.Log.e(TAG, l);
        } else {
            return 0;
        }
    }
    public static int wtf(String TAG, Object... log) {
        Metrics.wtf++;
        String l = wrapLog(joinObjects(log));
        FirebaseCrashlyticsProvider.wtf(TAG, l);
        if (enabled) {
            addLog(new LogItem(WTF, TAG, l));
            return android.util.Log.wtf(TAG, l);
        } else {
            return 0;
        }
    }
    public static int wtf(Throwable throwable) {
        Metrics.wtf++;
        FirebaseCrashlyticsProvider.wtf(throwable);
        if (enabled) {
            addLog(new LogItem(WTF_EXCEPTION, throwable));
            return android.util.Log.wtf("Assert", wrapLog(null), throwable);
        } else {
            return 0;
        }
    }
    public static int exception(Throwable throwable) {
        return exception(null, throwable);
    }
    public static int exception(String msg, Throwable throwable) {
        Metrics.exception++;
        msg = wrapLog(msg);
        FirebaseCrashlyticsProvider.exception(throwable);
        if (enabled) {
            addLog(new LogItem(EXCEPTION, throwable));
            return android.util.Log.e("Exception", msg, throwable);
        } else {
            return 0;
        }
    }
    private static void addLog(LogItem logItem) {
        try {
            if (logList == null) {
                logList = new ArrayList<>();
            }
            if (enabled) {
                logList.add(logItem);
            }
        } catch (Throwable throwable) {
            android.util.Log.e("Log.addLog", null, throwable);
        }
    }

    @NonNull
    private static String wrapLog(String log) {
        return "[" + Thread.currentThread().getName() + ":" + Thread.currentThread().getId() + "] " + (log == null ? "" : log);
    }
    @NonNull
    private static String joinObjects(Object... log) {
        StringBuilder sb = new StringBuilder();
        for (Object item : log) {
            if (item == null) {
                sb.append("<null>");
            } else if (item instanceof String) {
                sb.append(lString((String) item));
            } else if (item instanceof Boolean) {
                sb.append((boolean) item);
            } else if (item instanceof Integer) {
                sb.append((int) item);
            } else if (item instanceof Throwable) {
                sb.append(lThrow((Throwable) item));
            } else if (item instanceof Double) {
                sb.append((double) item);
            } else if (item instanceof Float) {
                sb.append((float) item);
            } else if (item instanceof Long) {
                sb.append((long) item);
            } else if (item instanceof Context || item instanceof Fragment || item instanceof JSONObject || item instanceof JSONArray) {
                sb.append(lNull(item));
            } else {
                sb.append(item);
            }
        }
        return sb.toString();
    }

    @NonNull
    public static String lString(String str) {
        return str == null ? "<null>" : str;
    }
    @NonNull
    public static String lBool(boolean bool) {
        return bool ? "true" : "false";
    }
    @NonNull
    public static String lNull(Object o) {
        return o == null ? "<null>" : "<notnull>";
    }
    @NonNull
    public static String lThrow(Throwable throwable) {
        return throwable != null ? throwable.getMessage() : "<empty>";
    }
}
