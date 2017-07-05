package com.bukhmastov.cdoitmo.utils;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Log {

    private static final int VERBOSE = android.util.Log.VERBOSE;
    private static final int DEBUG = android.util.Log.DEBUG;
    private static final int INFO = android.util.Log.INFO;
    private static final int WARN = android.util.Log.WARN;
    private static final int ERROR = android.util.Log.ERROR;
    private static final int EXCEPTION = 7331;
    private static final int WTF = android.util.Log.ASSERT;
    private static final int WTF_EXCEPTION = 8442;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.getDefault());
    private static ArrayList<LogItem> logList = null;
    @Nullable
    public static ArrayList<LogItem> getRawLog(){
        try {
            if (logList == null) {
                logList = new ArrayList<>();
            }
            return logList;
        } catch (Throwable t) {
            android.util.Log.e("Log.getRawLog", null, t);
            return null;
        }
    }
    @NonNull
    public static String getLog(){
        return getLog(true);
    }
    @NonNull
    public static String getLog(boolean reverse){
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
    public static ExtraLog getExtraLog(){
        ExtraLog extraLog = new ExtraLog();
        for (int i = 0; i < logList.size(); i++) {
            LogItem logItem = logList.get(i);
            switch (logItem.type) {
                case WARN: extraLog.warn++; break;
                case ERROR: extraLog.error++; break;
                case EXCEPTION: extraLog.exception++; break;
                case WTF: case WTF_EXCEPTION: extraLog.wtf++; break;
            }
        }
        extraLog.log = getLog(false);
        extraLog.log_reverse = getLog(true);
        return extraLog;
    }
    private static void addLog(LogItem logItem){
        try {
            if (logList == null) {
                logList = new ArrayList<>();
            }
            logList.add(logItem);
        } catch (Throwable throwable) {
            FirebaseCrashProvider.exception(throwable);
            android.util.Log.e("Log.addLog", null, throwable);
        }
    }

    public static int v(String TAG, String log){
        FirebaseCrashProvider.v(TAG, log);
        addLog(new LogItem(VERBOSE, TAG, log));
        return android.util.Log.v(TAG, log);
    }
    public static int d(String TAG, String log){
        FirebaseCrashProvider.d(TAG, log);
        addLog(new LogItem(DEBUG, TAG, log));
        return android.util.Log.d(TAG, log);
    }
    public static int i(String TAG, String log){
        FirebaseCrashProvider.i(TAG, log);
        addLog(new LogItem(INFO, TAG, log));
        return android.util.Log.i(TAG, log);
    }
    public static int w(String TAG, String log){
        FirebaseCrashProvider.w(TAG, log);
        addLog(new LogItem(WARN, TAG, log));
        return android.util.Log.w(TAG, log);
    }
    public static int e(String TAG, String log){
        FirebaseCrashProvider.e(TAG, log);
        addLog(new LogItem(ERROR, TAG, log));
        return android.util.Log.e(TAG, log);
    }
    public static int wtf(String TAG, String log){
        FirebaseCrashProvider.wtf(TAG, log);
        addLog(new LogItem(WTF, TAG, log));
        return android.util.Log.wtf(TAG, log);
    }
    public static int wtf(Throwable throwable){
        FirebaseCrashProvider.wtf(throwable);
        addLog(new LogItem(WTF_EXCEPTION, throwable));
        return android.util.Log.wtf("Assert", null, throwable);
    }
    public static int exception(Throwable throwable){
        FirebaseCrashProvider.exception(throwable);
        addLog(new LogItem(EXCEPTION, throwable));
        return android.util.Log.e("Exception", null, throwable);
    }

    private static class LogItem {
        public int type;
        public long ts;
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
    public static class ExtraLog {
        public int warn = 0;
        public int error = 0;
        public int exception = 0;
        public int wtf = 0;
        public String log = "";
        public String log_reverse = "";
    }

}
