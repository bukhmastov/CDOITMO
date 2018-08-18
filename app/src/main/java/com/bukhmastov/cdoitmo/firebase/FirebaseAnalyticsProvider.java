package com.bukhmastov.cdoitmo.firebase;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public interface FirebaseAnalyticsProvider {

    class Event {
        // 500 different types of Events | 40 characters long
        public static final String APP_OPEN = "cdo_app_open";                                   // приложение запущено
        public static final String LOGIN_REQUIRED = "cdo_login_required";                       // нужна авторизация
        public static final String LOGIN = "cdo_login";                                         // авторизован
        public static final String LOGOUT = "cdo_logout";                                       // разлогинен
        public static final String APP_VIEW = "cdo_app_view";                                   // переход на новый экран
        public static final String SHORTCUT_INSTALL = "cdo_shortcut_install";                   // установка ярлыка
        public static final String SHORTCUT_USE = "cdo_shortcut_use";                           // использование ярлыка
        public static final String WIDGET_INSTALL = "cdo_widget_install";                       // установка виджета расписания
        public static final String SCHEDULE_LESSON_ADD = "cdo_schedule_lesson_add";             // ручное добавление занятия в расписание
        public static final String SCHEDULE_LESSON_REDUCE = "cdo_schedule_lesson_reduce";       // скрытие занятия в расписании
        public static final String ROOM101_REQUEST_ADDED = "cdo_room101_request_added";         // оставлен запрос на тестирование в 101 кабинете
        public static final String ROOM101_REQUEST_DENIED = "cdo_room101_request_denied";       // отозван запрос на тестирование в 101 кабинете
        public static final String WIDGET_USAGE = "cdo_widget_usage";                           // использование виджета
        public static final String SHARE = "cdo_share";                                         // поделился контентом
        public static final String RECEIVE = "cdo_receive";                                     // принял контент
        public static final String EVENT = "cdo_event";                                         // обычное событие, не подходящее под остальные типы событий
    }
    class Param {
        // 25 unique Params with each Event type | 40 characters long | values 100 characters long
        public static final String LOGIN_COUNT = "cdo_login_count";
        public static final String LOGIN_NEW = "cdo_login_new";
        public static final String APP_VIEW_SCREEN = "cdo_view_screen";
        public static final String SHORTCUT_TYPE = "cdo_shortcut_type";
        public static final String WIDGET_QUERY = "cdo_widget_query";
        public static final String LESSON_TITLE = "cdo_lesson_title";
        public static final String ROOM101_REQUEST_DETAILS = "cdo_room101_request_details";
        public static final String WIDGET_USAGE_INFO = "cdo_widget_usage_info";
        public static final String EVENT_EXTRA = "cdo_event_extra";
        public static final String TYPE = "cdo_type";
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
