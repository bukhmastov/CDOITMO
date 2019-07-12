package com.bukhmastov.cdoitmo.firebase.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.google.firebase.perf.FirebasePerformance;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.Lazy;

public class FirebasePerformanceProviderImpl implements FirebasePerformanceProvider {

    private static final String TAG = "FirebasePerformanceProvider";
    private boolean enabled = false;
    private FirebasePerformance firebasePerformance = null;
    private String uuid = null;
    private Map<String, com.google.firebase.perf.metrics.Trace> traceMap = new HashMap<>();

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Lazy<Static> staticUtil;
    @Inject
    Lazy<FirebaseConfigProvider> firebaseConfigProvider;

    public FirebasePerformanceProviderImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    private FirebasePerformance getFirebasePerformance() {
        if (firebasePerformance == null) {
            firebasePerformance = FirebasePerformance.getInstance();
        }
        return firebasePerformance;
    }

    @Override
    public void setEnabled(Context context) {
        thread.assertNotUI();
        log.i(TAG, "Firebase Performance fetching status");
        firebaseConfigProvider.get().getString(context, FirebaseConfigProvider.PERFORMANCE_ENABLED, value -> {
            setEnabled(context, "1".equals(value));
        });
    }

    @Override
    public void setEnabled(Context context, boolean enabled) {
        try {
            this.enabled = enabled;
            this.uuid = staticUtil.get().getUUID(context);
            if (!enabled) {
                stopAll();
            }
            getFirebasePerformance().setPerformanceCollectionEnabled(this.enabled);
            log.i(TAG, "Firebase Performance ", (this.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public String startTrace(@TRACE String name) {
        try {
            if (!enabled) {
                return null;
            }
            name = name != null ? name : Trace.UNKNOWN;
            String key;
            do {
                key = name + "_" + StringUtils.getRandomString(8);
            } while (traceMap.containsKey(key));
            com.google.firebase.perf.metrics.Trace trace = getFirebasePerformance().newTrace(name);
            traceMap.put(key, trace);
            trace.putAttribute("user_id", uuid);
            trace.start();
            return key;
        } catch (Exception e) {
            log.exception(e);
            return null;
        }
    }

    @Override
    public boolean stopTrace(String key) {
        try {
            if (!enabled || key == null) {
                return false;
            }
            com.google.firebase.perf.metrics.Trace trace = traceMap.get(key);
            if (trace != null) {
                trace.stop();
                traceMap.remove(key);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public void stopAll() {
        for (Map.Entry<String, com.google.firebase.perf.metrics.Trace> entry : traceMap.entrySet()) {
            try {
                if (entry != null) {
                    com.google.firebase.perf.metrics.Trace trace = entry.getValue();
                    if (trace != null) {
                        trace.stop();
                    }
                }
            } catch (Exception ignore) {/* ignore */}
        }
        traceMap.clear();
    }

    @Override
    public void putAttribute(String key, String attr, Object... values) {
        try {
            if (!enabled || key == null || attr == null || values == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Object value : values) {
                try {
                    if (value instanceof String) {
                        sb.append((String) value);
                    } else {
                        sb.append(value.toString());
                    }
                } catch (Exception ignore) {
                    sb.append("<ERR>");
                }
            }
            putAttribute(key, attr, sb.toString());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void putAttribute(String key, String attr, String value) {
        try {
            if (!enabled || key == null || attr == null || value == null) {
                return;
            }
            com.google.firebase.perf.metrics.Trace trace = traceMap.get(key);
            if (trace != null) {
                String a = correctStringLength(attr, 32);
                String v = correctStringLength(value, 100);
                trace.putAttribute(a, v);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void putAttributeAndStop(String key, String attr, Object... values) {
        putAttribute(key, attr, values);
        stopTrace(key);
    }

    @Override
    public void putAttributeAndStop(String key, String attr, String value) {
        putAttribute(key, attr, value);
        stopTrace(key);
    }

    private String correctStringLength(String text, int len) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (len < 0 || len > text.length()) {
            return text;
        }
        return text.substring(0, len);
    }
}
