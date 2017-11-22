package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.Locale;

public class App extends Application {

    private static final String TAG = "Application";
    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.setEnabled(Storage.pref.get(this, "pref_allow_collect_logs", false));
            locale = Static.getLocale(this);
            Log.i(TAG, "Language | locale=" + locale.toString());
            setLocale();
            setFirebase();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            setLocale();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    private void setLocale() throws Throwable {
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void setFirebase() {
        FirebaseCrashProvider.setEnabled(this);
        FirebaseAnalyticsProvider.setEnabled(this);
    }
}

/*
 * --------------
 * TODO ИЗМЕНЕНИЯ - Обеспечение обратной совместимости | переход на версию 2.0
 * --------------
 * Раписание:
 *      * Корень json переработан
 *          OLD: query, type, title, label, pid, timestamp, cache_token, schedule
 *          NEW: schedule_type, query, type, title, timestamp, schedule
 * Расписание занятий:
 *      * Изменения в дне
 *          index -> weekday
 * Расписание экзаменов:
 *      * Экземпляр экзамена переработан
 *          OLD: subject, teacher, exam{date, time, room}, consult{date, time, room}
 *          NEW: subject, group, teacher, teacher_id, exam{date, time, room, building}, advice{date, time, room, building}
 * Скрытые и добавленные занятия:
 *      * Изменения в дне
 *          index -> weekday
 * Шаринг занятий
 *      * Изменение структуры
 *          OLD: type, content{query, title, token, added, reduced}
 *          NEW: type, version, content{query, title, added, reduced}
 * Кэш расписаний (любых) именуется query.toLowerCase() вместо [type + "_" + query]
 *
 *
 *
 */

/*
 * -------------
 * TODO ROAD MAP
 * -------------
 * -- Расписания --
 * Расписание экзаменов
 *      ...
 * Удаление устаревших классов и очистка
 * Обеспечение обратной совместимости
 *
 * -- Авторизация ИСУ --
 * ...
 *
 * -- ИСУ контент --
 * Информация о группе
 * Стипендии
 * Информация о неделе
 * Информация о пользователе
 *
 * -- Пофиксить баги из огненной базы --
 *
 */

/*
 * -------
 * СДЕЛАНО
 * -------
 * -- Расписания --
 * Расписание занятий
 *      Новый провайдер расписания
 *      Создание, редактирование и удаление занятий
 *      Фрагменты расписаний занятий:
 *          Разобраться с перезагрузкой фрагмента при resume (жизненный цикл)
 *          Добавить запоминание положения скролла
 *      Применить новое расписание в виджете
 *      Кэширование запросов расписания для предотвращения одновременного поиска по одинаковому запросу
 *      Применить новое расписание в приложении оставшегося времени
 *      Применить новое расписание в создании приложения оставшегося времени
 *      Применить новое расписание в настройках
 * Расписание занятий - старые классы удалены
 *
 */