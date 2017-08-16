package com.bukhmastov.cdoitmo.firebase;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FirebaseAnalyticsProvider {

    private static final String TAG = "FirebaseAnalyticsProvider";
    private static boolean enabled = true;
    private static FirebaseAnalytics firebaseAnalytics = null;

    public static class Event {
        // 500 different types of Events | 40 characters long
        public static String JOIN_GROUP = "cdo_join_group";                         // присоединен к определеннйо группе
        public static String APP_OPEN = "cdo_app_open";                             // приложение запущено
        public static String LOGIN = "cdo_login";                                   // авторизован
        public static String LOGOUT = "cdo_logout";                                 // разлогинен
        public static String APP_VIEW = "cdo_app_view";                             // переход на новый экран
        public static String SHORTCUT_INSTALL = "cdo_shortcut_install";             // установка ярлыка
        public static String SHORTCUT_USE = "cdo_shortcut_use";                     // использование ярлыка
        public static String WIDGET_INSTALL = "cdo_widget_install";                 // установка виджета расписания
        public static String SCHEDULE_LESSON_ADD = "cdo_schedule_lesson_add";       // ручное добавление занятия в расписание
        public static String SCHEDULE_LESSON_REDUCE = "cdo_schedule_lesson_reduce"; // скрытие занятия в расписании
        public static String ROOM101_REQUEST_ADDED = "cdo_room101_request_added";   // оставлен запрос на тестирование в 101 кабинете
        public static String ROOM101_REQUEST_DENIED = "cdo_room101_request_denied"; // отозван запрос на тестирование в 101 кабинете
        public static String EVENT = "cdo_event";                                   // обычное событие, не подходящее под остальные типы событий
    }
    public static class Param {
        // 25 unique Params with each Event type | 40 characters long | values 100 characters long
        public static String GROUP_ID = "cdo_group_id";
        public static String LOGIN_COUNT = "cdo_login_count";
        public static String LOGIN_NEW = "cdo_login_new";
        public static String APP_VIEW_SCREEN = "cdo_view_screen";
        public static String SHORTCUT_INFO = "cdo_shortcut_info";
        public static String WIDGET_QUERY = "cdo_widget_query";
        public static String LESSON_TITLE = "cdo_lesson_title";
        public static String ROOM101_REQUEST_DETAILS = "cdo_room101_request_details";
        public static String EVENT_EXTRA = "cdo_event_extra";
    }
    public static class Property {
        // 25 unique UserProperties | 24 characters long | values 36 characters long
        public static String FACULTY = "cdo_user_faculty";
        public static String LEVEL = "cdo_user_level";
        public static String COURSE = "cdo_user_course";
        public static String GROUP = "cdo_user_group";
        public static String THEME = "cdo_theme";
        public static String DEVICE = "cdo_device";
    }

    private static FirebaseAnalytics getFirebaseAnalytics(Context context) throws Exception {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
        return firebaseAnalytics;
    }

    public static boolean setEnabled(Context context) {
        return setEnabled(context, Storage.pref.get(context, "pref_allow_collect_analytics", true));
    }
    public static boolean setEnabled(Context context, boolean enabled) {
        return setEnabled(context, enabled, false);
    }
    public static boolean setEnabled(Context context, boolean enabled, boolean notify) {
        try {
            if (!enabled && notify) {
                FirebaseAnalyticsProvider.logEvent(
                        context,
                        FirebaseAnalyticsProvider.Event.JOIN_GROUP,
                        FirebaseAnalyticsProvider.getBundle(Param.GROUP_ID, "analytics_disabled")
                );
            }
            FirebaseAnalyticsProvider.enabled = enabled;
            getFirebaseAnalytics(context).setAnalyticsCollectionEnabled(FirebaseAnalyticsProvider.enabled);
            Log.i(TAG, "Firebase Analytics " + (FirebaseAnalyticsProvider.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Static.error(e);
        }
        return FirebaseAnalyticsProvider.enabled;
    }

    public static void setCurrentScreen(Activity activity) {
        setCurrentScreen(activity, null, null);
    }
    public static void setCurrentScreen(Activity activity, String screenOverride) {
        logCurrentScreen(activity, null, screenOverride);
    }
    public static void setCurrentScreen(Activity activity, Fragment fragment) {
        logCurrentScreen(activity, fragment, null);
    }
    public static void setCurrentScreen(final Activity activity, final Fragment fragment, final String view_screen) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    if (activity == null) return;
                    String vs = view_screen;
                    if (view_screen == null) {
                        if (fragment != null) {
                            vs = fragment.getClass().getSimpleName();
                        } else {
                            vs = activity.getClass().getSimpleName();
                        }
                    }
                    getFirebaseAnalytics(activity).setCurrentScreen(activity, vs, null);
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    public static void logCurrentScreen(Activity activity) {
        logCurrentScreen(activity, null, null);
    }
    public static void logCurrentScreen(Activity activity, String screenOverride) {
        logCurrentScreen(activity, null, screenOverride);
    }
    public static void logCurrentScreen(Activity activity, Fragment fragment) {
        logCurrentScreen(activity, fragment, null);
    }
    public static void logCurrentScreen(Activity activity, Fragment fragment, String view_screen) {
        try {
            if (!enabled) return;
            if (activity == null) return;
            if (view_screen == null) {
                if (fragment != null) {
                    view_screen = fragment.getClass().getSimpleName();
                } else {
                    view_screen = activity.getClass().getSimpleName();
                }
            }
            FirebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.APP_VIEW,
                    FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.APP_VIEW_SCREEN, view_screen)
            );
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public static void setUserProperties(Context context, String group) {
        try {
            Matcher m = Pattern.compile("(\\w)(\\d)(\\d)(\\d)(\\d)(\\w?)").matcher(group);
            if (m.find()) {
                String faculty = m.group(1).trim().toUpperCase();
                String level = m.group(2).trim();
                String course = m.group(3).trim();
                switch (faculty) {
                    case "A": faculty = "A - Естественнонаучный"; break;
                    case "B": faculty = "B - Лазерной и световой инженерии"; break;
                    case "C": faculty = "C - Дизайна и урбанистики"; break;
                    case "D": faculty = "D - ИМРиП"; break;
                    case "F": faculty = "F - Трансляционной медицины"; break;
                    case "K": faculty = "K - Инфокоммуникационных технологий"; break;
                    case "M": faculty = "M - ИТиП"; break;
                    case "N": faculty = "N - 'ИКВО'"; break;
                    case "O": faculty = "O - 'ИМБиП'"; break;
                    case "P": faculty = "P - КТиУ"; break;
                    case "S": faculty = "S - 'Академия ЛИМТУ'"; break;
                    case "T": faculty = "T - ФПБИ"; break;
                    case "U": faculty = "U - ФТМИ"; break;
                    case "V": faculty = "V - Фотоники и оптоинформатики"; break;
                    case "W": faculty = "W - ФХКТК"; break;
                    case "X": faculty = "X - Заочный"; break;
                    case "Y": faculty = "Y - Среднего проф. образования"; break;
                }
                switch (level) {
                    case "0": level = "0 - Подг. отделение для ин. граждан"; break;
                    case "2": level = "2 - Среднее проф. образование"; break;
                    case "3": level = "3 - Бакалавриат"; break;
                    case "4": level = "4 - Магистратура"; break;
                    case "5": level = "5 - Специалитет"; break;
                    case "6": level = "6 - Аспирантура"; break;
                    case "9": level = "9 - Дополнительное образование"; break;
                }
                course = course + " " + "курс";
                setUserProperty(context, Property.FACULTY, faculty);
                setUserProperty(context, Property.LEVEL, level);
                setUserProperty(context, Property.COURSE, course);
            }
            setUserProperty(context, Property.GROUP, group);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    public static void setUserProperty(final Context context, final String property, final String value) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    getFirebaseAnalytics(context).setUserProperty(property, value);
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    public static void logEvent(Context context, String name) {
        logEvent(context, name, null);
    }
    public static void logEvent(final Context context, final String name, final Bundle params) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    getFirebaseAnalytics(context).logEvent(name, params);
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }
    public static void logBasicEvent(final Context context, final String content) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    getFirebaseAnalytics(context).logEvent(
                            Event.EVENT,
                            FirebaseAnalyticsProvider.getBundle(Param.EVENT_EXTRA, content)
                    );
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    public static Bundle getBundle(String key, Object value) {
        return getBundle(key, value, null);
    }
    public static Bundle getBundle(String key, int value) {
        return getBundle(key, value, null);
    }
    public static Bundle getBundle(String key, int value, Bundle bundle) {
        return getBundle(key, (Integer) value, bundle);
    }
    public static Bundle getBundle(String key, Object value, Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (value instanceof String) {
            bundle.putString(key, (String) value);
        }
        if (value instanceof Integer) {
            bundle.putInt(key, (Integer) value);
        }
        return bundle;
    }
}
