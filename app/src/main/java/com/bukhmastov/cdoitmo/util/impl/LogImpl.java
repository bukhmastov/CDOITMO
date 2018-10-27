package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.singleton.LogMetrics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;

public class LogImpl implements Log {

    private static final String TAGD = "TAG_DEBUG";

    private static class LogItem {
        public final int type;
        public final long ts;
        public String TAG;
        public String log;
        public Throwable throwable;
        public LogItem(int type, String TAG, String log) {
            this.type = type;
            this.ts = System.currentTimeMillis();
            this.TAG = TAG;
            this.log = log;
        }
        public LogItem(int type, Throwable throwable) {
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

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.getDefault());
    private ArrayList<LogItem> logList = null;
    private boolean enabled = false;

    @Inject
    Lazy<FirebaseCrashlyticsProvider> firebaseCrashlyticsProvider;

    public LogImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!this.enabled && logList != null) {
            logList.clear();
        }
    }

    @Override
    public int v(String TAG, Object... log) {
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().v(TAG, l);
        if (enabled) {
            addLog(new LogItem(VERBOSE, TAG, l));
            return android.util.Log.v(TAG, l);
        } else {
            return 0;
        }
    }

    @Override
    public int d(Object... log) {
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().d(TAGD, l);
        if (enabled) {
            addLog(new LogItem(DEBUG, TAGD, l));
            return android.util.Log.d(TAGD, l);
        } else {
            return 0;
        }
    }

    @Override
    public int i(String TAG, Object... log) {
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().i(TAG, l);
        if (enabled) {
            addLog(new LogItem(INFO, TAG, l));
            return android.util.Log.i(TAG, l);
        } else {
            return 0;
        }
    }

    @Override
    public int w(String TAG, Object... log) {
        LogMetrics.warn++;
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().w(TAG, l);
        if (enabled) {
            addLog(new LogItem(WARN, TAG, l));
            return android.util.Log.w(TAG, l);
        } else {
            return 0;
        }
    }

    @Override
    public int e(String TAG, Object... log) {
        LogMetrics.error++;
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().e(TAG, l);
        if (enabled) {
            addLog(new LogItem(ERROR, TAG, l));
            return android.util.Log.e(TAG, l);
        } else {
            return 0;
        }
    }

    @Override
    public int wtf(String TAG, Object... log) {
        LogMetrics.wtf++;
        String l = wrapLog(joinObjects(log));
        firebaseCrashlyticsProvider.get().wtf(TAG, l);
        if (enabled) {
            addLog(new LogItem(WTF, TAG, l));
            return android.util.Log.wtf(TAG, l);
        } else {
            return 0;
        }
    }

    @Override
    public int wtf(Throwable throwable) {
        LogMetrics.wtf++;
        firebaseCrashlyticsProvider.get().wtf(throwable);
        if (enabled) {
            addLog(new LogItem(WTF_EXCEPTION, throwable));
            return android.util.Log.wtf("Assert", wrapLog(null), throwable);
        } else {
            return 0;
        }
    }

    @Override
    public int exception(Throwable throwable) {
        return exception(null, throwable);
    }

    @Override
    public int exception(String msg, Throwable throwable) {
        LogMetrics.exception++;
        msg = wrapLog(msg);
        firebaseCrashlyticsProvider.get().exception(throwable);
        if (enabled) {
            addLog(new LogItem(EXCEPTION, throwable));
            return android.util.Log.e("Exception", msg, throwable);
        } else {
            return 0;
        }
    }

    @NonNull
    @Override
    public String getLog() {
        return getLog(true);
    }

    @NonNull
    @Override
    public String getLog(boolean reverse) {
        if (logList == null) {
            logList = new ArrayList<>();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("-------Device--------").append("\n");
        sb.append("DEVICE: ").append(Build.DEVICE).append("\n");
        sb.append("MODEL: ").append(Build.MODEL).append("\n");
        sb.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
        sb.append("DISPLAY: ").append(Build.DISPLAY).append("\n");
        sb.append("SDK_INT: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("-----Application-----").append("\n");
        sb.append(App.versionName).append(" (").append(App.versionCode).append(")").append("\n");
        sb.append("---------Log---------").append("\n");
        for (int i = reverse ? (logList.size() - 1) : 0; reverse ? (i >= 0) : (i < logList.size()); i = reverse ? (i - 1) : (i + 1)) {
            LogItem logItem = logList.get(i);
            if (logItem == null) {
                continue;
            }
            sb.append(dateFormat.format(logItem.ts)).append(" | ").append(level2String(logItem.type)).append("/");
            if ((logItem.type == EXCEPTION || logItem.type == WTF_EXCEPTION) && logItem.throwable != null) {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    try (PrintStream ps = new PrintStream(os)) {
                        logItem.throwable.printStackTrace(ps);
                    }
                    sb.append(new String(os.toByteArray(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    sb.append("~~ FAILED TO APPEND STACK TRACE ~~").append("\n").append(e.getMessage());
                }
            } else {
                sb.append(logItem.TAG).append(" ").append(logItem.log);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void addLog(LogItem logItem) {
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
    private String level2String(int level) {
        switch (level) {
            case VERBOSE: return "VERBOSE";
            case DEBUG: return "DEBUG";
            case INFO: return "INFO";
            case WARN: return "WARN";
            case ERROR: return "ERROR";
            case EXCEPTION: return "EXCEPTION";
            case WTF: return "WTF";
            case WTF_EXCEPTION: return "WTF_EXCEPTION";
        }
        return "UNKNOWN";
    }

    @NonNull
    private String wrapLog(String log) {
        return "[" + java.lang.Thread.currentThread().getName() + ":" + java.lang.Thread.currentThread().getId() + "] " + (log == null ? "" : log);
    }

    @NonNull
    private String joinObjects(Object... log) {
        StringBuilder sb = new StringBuilder();
        for (Object item : log) {
            if (item == null) {
                sb.append("<null>");
            } else if (item instanceof String) {
                sb.append((String) item);
            } else if (item instanceof Boolean) {
                sb.append(item.equals(Boolean.TRUE) ? "true" : "false");
            } else if (item instanceof Integer) {
                sb.append((int) item);
            } else if (item instanceof JSONObject) {
                sb.append("<jsonObject length=").append(((JSONObject) item).length()).append(">");
            } else if (item instanceof JSONArray) {
                sb.append("<jsonArray length=").append(((JSONArray) item).length()).append(">");
            } else if (item instanceof Throwable) {
                sb.append(((Throwable) item).getMessage());
            } else if (item instanceof Double) {
                sb.append((double) item);
            } else if (item instanceof Float) {
                sb.append((float) item);
            } else if (item instanceof Long) {
                sb.append((long) item);
            } else if (item instanceof Context || item instanceof Fragment) {
                sb.append("<notnull>");
            } else {
                sb.append(item);
            }
        }
        return sb.toString();
    }
}
