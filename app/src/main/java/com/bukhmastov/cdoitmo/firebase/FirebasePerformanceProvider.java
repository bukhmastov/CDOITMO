package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.google.firebase.perf.FirebasePerformance;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

public class FirebasePerformanceProvider {

    private static final String TAG = "FirebasePerformanceProvider";
    private static boolean enabled = false;
    private static FirebasePerformance firebasePerformance = null;
    private static String uuid = null;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Trace.UNKNOWN, Trace.LOGIN, Trace.LOGOUT, Trace.PROTOCOL_TRACKER,
            Trace.Storage.PUT, Trace.Storage.GET, Trace.Storage.DELETE, Trace.Storage.CLEAR, Trace.Storage.EXISTS, Trace.Storage.RESET, Trace.Storage.LIST, Trace.Storage.SIZE,
            Trace.Schedule.LESSONS, Trace.Schedule.EXAMS, Trace.Schedule.ATTESTATIONS,
            Trace.Parse.RATING, Trace.Parse.RATING_LIST, Trace.Parse.RATING_TOP_LIST, Trace.Parse.ROOM_101_DATE_PICK, Trace.Parse.ROOM_101_TIME_END_PICK,
            Trace.Parse.ROOM_101_TIME_START_PICK, Trace.Parse.ROOM_101_VIEW_REQUEST, Trace.Parse.SCHEDULE_ATTESTATIONS, Trace.Parse.SCHEDULE_EXAMS_GROUP,
            Trace.Parse.SCHEDULE_EXAMS_TEACHER, Trace.Parse.USER_DATA,
            Trace.Convert.EREGISTER, Trace.Convert.PROTOCOL, Trace.Convert.Schedule.LESSONS, Trace.Convert.Schedule.EXAMS, Trace.Convert.Schedule.TEACHERS, Trace.Convert.Schedule.ADDITIONAL,
    })
    public @interface TRACE {}
    public static class Trace {
        public static final String UNKNOWN = "unknown";
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
        public static final String PROTOCOL_TRACKER = "protocol_tracker";
        public static class Storage {
            public static final String PUT = "storage_put";
            public static final String GET = "storage_get";
            public static final String DELETE = "storage_delete";
            public static final String CLEAR = "storage_clear";
            public static final String EXISTS = "storage_exists";
            public static final String RESET = "storage_reset";
            public static final String LIST = "storage_list";
            public static final String SIZE = "storage_size";
        }
        public static class Schedule {
            public static final String LESSONS = "schedule_lessons";
            public static final String EXAMS = "schedule_exams";
            public static final String ATTESTATIONS = "schedule_attestations";
        }
        public static class Parse {
            public static final String RATING = "parse_rating";
            public static final String RATING_LIST = "parse_rating_list";
            public static final String RATING_TOP_LIST = "parse_rating_top_list";
            public static final String ROOM_101_DATE_PICK = "parse_room_101_date";
            public static final String ROOM_101_TIME_END_PICK = "parse_room_101_time_end";
            public static final String ROOM_101_TIME_START_PICK = "parse_room_101_time_start";
            public static final String ROOM_101_VIEW_REQUEST = "parse_room_101_view_request";
            public static final String SCHEDULE_ATTESTATIONS = "parse_schedule_attestations";
            public static final String SCHEDULE_EXAMS_GROUP = "parse_schedule_exams_group";
            public static final String SCHEDULE_EXAMS_TEACHER = "parse_schedule_exams_teacher";
            public static final String USER_DATA = "parse_user_data";
        }
        public static class Convert {
            public static final String EREGISTER = "convert_eregister";
            public static final String PROTOCOL = "convert_protocol";
            public static class Schedule {
                public static final String LESSONS = "convert_schedule_lessons";
                public static final String EXAMS = "convert_schedule_exams";
                public static final String TEACHERS = "convert_schedule_teachers";
                public static final String ADDITIONAL = "convert_schedule_additional";
            }
        }
    }

    private static FirebasePerformance getFirebasePerformance() {
        if (firebasePerformance == null) {
            firebasePerformance = FirebasePerformance.getInstance();
        }
        return firebasePerformance;
    }

    public static void setEnabled(Context context) {
        Static.T.runThread(() -> {
            Log.i(TAG, "Firebase Performance fetching status");
            FirebaseConfigProvider.getString(FirebaseConfigProvider.PERFORMANCE_ENABLED, value -> Static.T.runThread(() -> setEnabled(context, "1".equals(value))));
        });
    }
    public static void setEnabled(Context context, boolean enabled) {
        try {
            FirebasePerformanceProvider.enabled = enabled;
            FirebasePerformanceProvider.uuid = Static.getUUID(context);
            if (!enabled) {
                FirebasePerformanceProvider.stopAll();
            }
            getFirebasePerformance().setPerformanceCollectionEnabled(FirebasePerformanceProvider.enabled);
            Log.i(TAG, "Firebase Performance ", (FirebasePerformanceProvider.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private static Map<String, com.google.firebase.perf.metrics.Trace> traceMap = new HashMap<>();

    public static String startTrace(@TRACE String name) {
        try {
            if (!enabled) {
                return null;
            }
            name = name != null ? name : Trace.UNKNOWN;
            String key;
            do {
                key = name + "_" + Static.getRandomString(8);
            } while (traceMap.containsKey(key));
            com.google.firebase.perf.metrics.Trace trace = getFirebasePerformance().newTrace(name);
            traceMap.put(key, trace);
            trace.putAttribute("user_id", uuid);
            trace.start();
            return key;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    public static boolean stopTrace(String key) {
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
            Static.error(e);
            return false;
        }
    }
    public static void stopAll() {
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

    public static void putAttribute(String key, String attr, Object... values) {
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
            Static.error(e);
        }
    }
    public static void putAttribute(String key, String attr, String value) {
        try {
            if (!enabled || key == null || attr == null || value == null) {
                return;
            }
            com.google.firebase.perf.metrics.Trace trace = traceMap.get(key);
            if (trace != null) {
                attr = attr.trim();
                value = value.trim();
                if (attr.length() > 40) {
                    attr = attr.substring(0, 40);
                }
                if (value.length() > 100) {
                    value = value.substring(0, 100);
                }
                trace.putAttribute(attr, value);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public static void putAttributeAndStop(String key, String attr, Object... values) {
        putAttribute(key, attr, values);
        stopTrace(key);
    }
    public static void putAttributeAndStop(String key, String attr, String value) {
        putAttribute(key, attr, value);
        stopTrace(key);
    }
}
