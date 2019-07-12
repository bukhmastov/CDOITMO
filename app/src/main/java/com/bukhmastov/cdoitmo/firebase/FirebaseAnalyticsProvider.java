package com.bukhmastov.cdoitmo.firebase;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

public interface FirebaseAnalyticsProvider {

    class Event {
        // 500 different types of Events | 40 characters long
        public static final String APP_OPEN = "cdo_app_open";                                   // приложение запущено
        public static final String LOGIN_REQUIRED = "cdo_login_required";                       // нужна авторизация
        public static final String LOGIN = "cdo_login";                                         // авторизован
        public static final String LOGOUT = "cdo_logout";                                       // разлогинен
        public static final String LOGIN_FAILED = "cdo_login_failed";                           // не авторизован
        public static final String APP_VIEW = "cdo_app_view";                                   // переход на новый экран
        public static final String SHORTCUT_INSTALL = "cdo_shortcut_install";                   // установка ярлыка
        public static final String SHORTCUT_USE = "cdo_shortcut_use";                           // использование ярлыка
        public static final String WIDGET_INSTALL = "cdo_widget_install";                       // установка виджета расписания
        public static final String ROOM101_REQUEST_ADDED = "cdo_room101_request_added";         // оставлен запрос на тестирование в 101 кабинете
        public static final String ROOM101_REQUEST_DENIED = "cdo_room101_request_denied";       // отозван запрос на тестирование в 101 кабинете
        public static final String WIDGET_USAGE = "cdo_widget_usage";                           // использование виджета
        public static final String SHARE = "cdo_share";                                         // поделился контентом
        public static final String RECEIVE = "cdo_receive";                                     // принял контент
        public static final String EVENT = "cdo_event";                                         // обычное событие, не подходящее под остальные типы событий
        public static final String LOGIN_ISU = "cdo_isu_login";                                 // авторизован в ису
        public static final String LOGOUT_ISU = "cdo_isu_logout";                               // разлогинен из ису
        public static final String LOGIN_ISU_FAILED = "cdo_isu_login_failed";                   // не авторизован в ису
        public static final String SCHEDULE_LESSONS = "cdo_schedule_lessons";                   // использование расписания занятий
    }
    class Param {
        // 25 unique Params with each Event type | 40 characters long | values 100 characters long
        public static final String LOGIN_COUNT = "cdo_login_count";
        public static final String LOGIN_NEW = "cdo_login_new";
        public static final String APP_VIEW_SCREEN = "cdo_view_screen";
        public static final String SHORTCUT_TYPE = "cdo_shortcut_type";
        public static final String WIDGET_QUERY = "cdo_widget_query";
        public static final String ROOM101_REQUEST_DETAILS = "cdo_room101_request_details";
        public static final String WIDGET_USAGE_INFO = "cdo_widget_usage_info";
        public static final String EVENT_EXTRA = "cdo_event_extra";
        public static final String TYPE = "cdo_type";
        public static final String SCHEDULE_LESSONS_TYPE = "cdo_schedule_lessons_type";
        public static final String SCHEDULE_LESSONS_PARITY = "cdo_schedule_lessons_parity";
        public static final String SCHEDULE_LESSONS_QUERY = "cdo_schedule_lessons_query";
        public static final String SCHEDULE_LESSONS_QUERY_IS_SELF = "cdo_schedule_lessons_query_is_self";
        public static final String SCHEDULE_LESSONS_EXTRA = "cdo_schedule_lessons_extra";
    }
    class Property {
        // 25 unique UserProperties | 24 characters long | values 36 characters long
        public static final String FACULTY = "cdo_user_faculty";
        public static final String LEVEL = "cdo_user_level";
        public static final String COURSE = "cdo_user_course";
        public static final String GROUP = "cdo_user_group";
        public static final String THEME = "cdo_theme";
        public static final String DEVICE = "cdo_device";
    }

    boolean setEnabled(Context context);
    boolean setEnabled(Context context, boolean enabled);
    boolean setEnabled(Context context, boolean enabled, boolean notify);

    void setCurrentScreen(Activity activity);
    void setCurrentScreen(Activity activity, String screenOverride);
    void setCurrentScreen(Activity activity, Fragment fragment);
    void setCurrentScreen(Activity activity, Fragment fragment, String view_screen);

    void logCurrentScreen(Activity activity);
    void logCurrentScreen(Activity activity, String screenOverride);
    void logCurrentScreen(Activity activity, Fragment fragment);
    void logCurrentScreen(Activity activity, Fragment fragment, String view_screen);

    void setUserProperties(Context context, String group);
    void setUserProperty(Context context, String property, String value);

    void logEvent(Context context, String name);
    void logEvent(Context context, String name, Bundle params);
    void logBasicEvent(Context context, String content);

    Bundle getBundle(String key, Object value);
    Bundle getBundle(String key, int value);
    Bundle getBundle(String key, int value, Bundle bundle);
    Bundle getBundle(String key, Object value, Bundle bundle);
}
