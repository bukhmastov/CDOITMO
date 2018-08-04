package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface FirebasePerformanceProvider {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Trace.UNKNOWN, Trace.LOGIN, Trace.LOGOUT, Trace.PROTOCOL_TRACKER,
            Trace.Storage.PUT, Trace.Storage.GET, Trace.Storage.DELETE, Trace.Storage.CLEAR, Trace.Storage.EXISTS, Trace.Storage.LIST, Trace.Storage.SIZE,
            Trace.Schedule.LESSONS, Trace.Schedule.EXAMS, Trace.Schedule.ATTESTATIONS,
            Trace.Parse.RATING, Trace.Parse.RATING_LIST, Trace.Parse.RATING_TOP_LIST, Trace.Parse.ROOM_101_DATE_PICK, Trace.Parse.ROOM_101_TIME_END_PICK,
            Trace.Parse.ROOM_101_TIME_START_PICK, Trace.Parse.ROOM_101_VIEW_REQUEST, Trace.Parse.SCHEDULE_ATTESTATIONS, Trace.Parse.SCHEDULE_EXAMS_GROUP,
            Trace.Parse.SCHEDULE_EXAMS_TEACHER, Trace.Parse.USER_DATA,
            Trace.Convert.EREGISTER, Trace.Convert.PROTOCOL, Trace.Convert.Schedule.LESSONS, Trace.Convert.Schedule.EXAMS, Trace.Convert.Schedule.TEACHERS, Trace.Convert.Schedule.ADDITIONAL,
    })
    @interface TRACE {}
    class Trace {
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

    void setEnabled(Context context);
    void setEnabled(Context context, boolean enabled);

    String startTrace(@TRACE String name);
    boolean stopTrace(String key);
    void stopAll();

    void putAttribute(String key, String attr, Object... values);
    void putAttribute(String key, String attr, String value);

    void putAttributeAndStop(String key, String attr, Object... values);
    void putAttributeAndStop(String key, String attr, String value);
}
