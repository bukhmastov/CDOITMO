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
    }

    public static class Param {
        public static String GROUP_ID = "cdo_group_id";
        public static String LOGIN_COUNT = "cdo_login_count";
        public static String LOGIN_NEW = "cdo_login_new";
        public static String APP_VIEW_SCREEN = "cdo_view_screen";
        public static String SHORTCUT_INFO = "cdo_shortcut_info";
        public static String WIDGET_QUERY = "cdo_widget_query";
        public static String LESSON_TITLE = "cdo_lesson_title";
        public static String ROOM101_REQUEST_DETAILS = "cdo_room101_request_details";
    }

    public static class Property {
        public static String FACULTY = "cdo_user_faculty";
        public static String LEVEL = "cdo_user_level";
        public static String COURSE = "cdo_user_course";
        public static String GROUP = "cdo_user_group";
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

    public static void setCurrentScreen(Activity activity, Class screenClass) {
        try {
            if (!enabled) return;
            if (activity != null) {
                getFirebaseAnalytics(activity).setCurrentScreen(activity, screenClass.getSimpleName(), null);
            }
        } catch (Exception e) {
            Static.error(e);
        }
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
                if (fragment == null) {
                    view_screen = activity.getClass().getSimpleName();
                } else {
                    view_screen = fragment.getClass().getSimpleName();
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
                    case "C": faculty = "C - Институт дизайна и урбанистики"; break;
                    case "D": faculty = "D - Институт международного развития и партнерства"; break;
                    case "F": faculty = "F - Институт трансляционной медицины"; break;
                    case "K": faculty = "K - Инфокоммуникационных технологий"; break;
                    case "M": faculty = "M - Информационных технологий и программирования"; break;
                    case "N": faculty = "N - 'ИКВО'"; break;
                    case "O": faculty = "O - 'ИМБиП'"; break;
                    case "P": faculty = "P - Компьютерных технологий и управления"; break;
                    case "S": faculty = "S - Методов и техники управления 'Академия ЛИМТУ'"; break;
                    case "T": faculty = "T - Пищевых биотехнологий и инженерии"; break;
                    case "U": faculty = "U - Технорлогического менеджмента и инноваций"; break;
                    case "V": faculty = "V - Фотоники и оптоинформатики"; break;
                    case "W": faculty = "W - Холодильной, криогенной техники и кондиционирования"; break;
                    case "X": faculty = "X - Заочный"; break;
                    case "Y": faculty = "Y - Среднего профессионального образования"; break;
                }
                switch (level) {
                    case "0": level = "0 - Подготовительное отделение для иностранных граждан"; break;
                    case "2": level = "2 - Среднее профессиональное образование"; break;
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

    public static void setUserProperty(Context context, String property, String value) {
        try {
            if (!enabled) return;
            getFirebaseAnalytics(context).setUserProperty(property, value);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public static void logEvent(Context context, String name) {
        logEvent(context, name, null);
    }

    public static void logEvent(Context context, String name, Bundle params) {
        try {
            if (!enabled) return;
            getFirebaseAnalytics(context).logEvent(name, params);
        } catch (Exception e) {
            Static.error(e);
        }
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
