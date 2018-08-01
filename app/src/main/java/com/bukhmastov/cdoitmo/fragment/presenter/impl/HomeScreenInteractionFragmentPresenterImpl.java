package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.HomeScreenInteractionFragmentPresenter;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.receiver.ShortcutReceiver;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

public class HomeScreenInteractionFragmentPresenterImpl implements HomeScreenInteractionFragmentPresenter {

    private static final String TAG = "ShortcutCreateFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private ShortcutReceiver receiver = new ShortcutReceiver();
    private Client.Request requestHandle = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    ScheduleAttestations scheduleAttestations;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    private interface Result {
        void done(String title, String query);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PICK, WIDGETS, APPS, SHORTCUTS})
    private @interface MODE {}
    private static final String PICK = "pick";
    private static final String WIDGETS = "widgets";
    private static final String APPS = "apps";
    private static final String SHORTCUTS = "shortcuts";

    private class App {
        private final String id;
        private final String title;
        private final String desc;
        private final String desc_extra;
        private @DrawableRes
        final int image;
        private App(String id, String title, String desc, String desc_extra, int image) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.desc_extra = desc_extra;
            this.image = image;
        }
    }
    private class Shortcut {
        private final String id;
        private final String meta;
        private final String title;
        private final String desc;
        private @DrawableRes
        final int image;
        private Shortcut(String id, String meta, String title, String desc, int image) {
            this.id = id;
            this.meta = meta;
            this.title = title;
            this.desc = desc;
            this.image = image;
        }
    }
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<Shortcut> shortcuts = new ArrayList<>();
    
    public HomeScreenInteractionFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        ConnectedFragment.Data data = ConnectedFragment.getData(activity, this.getClass());
        if (data != null) {
            activity.updateToolbar(activity, data.title, data.image);
        }
        // Инициализируем приложения
        apps.clear();
        apps.add(new App("time_remaining_widget", activity.getString(R.string.time_remaining_widget), activity.getString(R.string.time_remaining_widget_desc), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_time_remaining_widget));
        apps.add(new App("days_remaining_widget", activity.getString(R.string.days_remaining_widget), activity.getString(R.string.days_remaining_widget_desc), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_days_remaining_widget));
        // Инициализируем ярлыки
        shortcuts.clear();
        shortcuts.add(new Shortcut("offline", null, activity.getString(R.string.app_name), activity.getString(R.string.launch_app_offline), R.mipmap.ic_shortcut_offline));
        shortcuts.add(new Shortcut("tab", "e_journal", activity.getString(R.string.e_journal), null, R.mipmap.ic_shortcut_e_journal));
        shortcuts.add(new Shortcut("tab", "protocol_changes", activity.getString(R.string.protocol_changes), null, R.mipmap.ic_shortcut_protocol_changes));
        shortcuts.add(new Shortcut("tab", "rating", activity.getString(R.string.rating), null, R.mipmap.ic_shortcut_rating));
        shortcuts.add(new Shortcut("tab", "room101", activity.getString(R.string.room101), null, R.mipmap.ic_shortcut_room101));
        shortcuts.add(new Shortcut("room101", "create", activity.getString(R.string.room101create), null, R.mipmap.ic_shortcut_room101_add));
        shortcuts.add(new Shortcut("schedule_lessons", null, activity.getString(R.string.schedule_lessons), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_schedule_lessons));
        shortcuts.add(new Shortcut("schedule_exams", null, activity.getString(R.string.schedule_exams), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_schedule_exams));
        shortcuts.add(new Shortcut("schedule_attestations", null, activity.getString(R.string.schedule_attestations), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_schedule_attestations));
        shortcuts.add(new Shortcut("university", null, activity.getString(R.string.university), activity.getString(R.string.need_to_choose_type), R.mipmap.ic_shortcut_university));
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        activity.registerReceiver(receiver, new IntentFilter(ShortcutReceiver.ACTION_ADD_SHORTCUT));
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        activity.unregisterReceiver(receiver);
    }

    @Override
    public void onViewCreated() {
        initPicker(true);
    }

    private void route(final @MODE String mode) {
        thread.run(() -> {
            log.v(TAG, "route | mode=" + mode);
            switch (mode) {
                case PICK: initPicker(false); break;
                case WIDGETS: initWidgets(); break;
                case APPS: initApps(); break;
                case SHORTCUTS: initShortcuts(); break;
            }
        });
    }

    private void initPicker(final boolean first_launch) {
        thread.runOnUI(() -> {
            log.v(TAG, "initPicker | first_launch=" + (first_launch ? "true" : "false"));
            try {
                // Переключаем режим отображения
                toggleMode(false, !first_launch);
                // Инициализируем кнопки
                ViewGroup menu_widgets = fragment.container().findViewById(R.id.menu_widgets);
                ViewGroup menu_apps = fragment.container().findViewById(R.id.menu_apps);
                ViewGroup menu_shortcuts = fragment.container().findViewById(R.id.menu_shortcuts);
                if (menu_widgets != null) {
                    menu_widgets.setOnClickListener(view -> route(WIDGETS));
                }
                if (menu_apps != null) {
                    menu_apps.setOnClickListener(view -> route(APPS));
                }
                if (menu_shortcuts != null) {
                    menu_shortcuts.setOnClickListener(view -> route(SHORTCUTS));
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initWidgets() {
        thread.run(() -> {
            log.v(TAG, "initWidgets");
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
                    if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                        AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
                        ComponentName componentName = appWidgetProviderInfo.provider;
                        if (!appWidgetManager.requestPinAppWidget(componentName, null, null)) {
                            showWidgetsHolder();
                        }
                    } else {
                        showWidgetsHolder();
                    }
                } else {
                    showWidgetsHolder();
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initApps() {
        thread.runOnUI(() -> {
            log.v(TAG, "initApps");
            try {
                // Переключаем режим отображения
                toggleMode(true);
                // Устанавливаем заголовок
                ImageView header_icon = fragment.container().findViewById(R.id.header_icon);
                TextView header_text = fragment.container().findViewById(R.id.header_text);
                ImageView header_close = fragment.container().findViewById(R.id.header_close);
                if (header_icon != null) {
                    header_icon.setImageResource(R.drawable.ic_extension);
                }
                if (header_text != null) {
                    header_text.setText(R.string.homescreen_apps);
                }
                if (header_close != null) {
                    header_close.setOnClickListener(view -> route(PICK));
                }
                // Отображаем приложения
                ViewGroup content = fragment.container().findViewById(R.id.content);
                if (content != null) {
                    content.removeAllViews();
                    for (final App app : apps) {
                        ViewGroup item = (ViewGroup) fragment.inflate(R.layout.layout_homescreen_apps_item);
                        ((ImageView) item.findViewById(R.id.image)).setImageResource(app.image);
                        ((TextView) item.findViewById(R.id.title)).setText(app.title);
                        ((TextView) item.findViewById(R.id.desc)).setText(app.desc);
                        ((TextView) item.findViewById(R.id.desc_extra)).setText(app.desc_extra);
                        item.setOnClickListener(view -> thread.run(() -> {
                            String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                            switch (app.id) {
                                case "time_remaining_widget": {
                                    getScheduleLessons(group.isEmpty() ? null : group, (title, query) -> {
                                        try {
                                            addShortcut(
                                                    app.id,
                                                    new JSONObject().put("label", title).put("query", query).toString(),
                                                    "regular"
                                            );
                                        } catch (Exception e) {
                                            log.exception(e);
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    });
                                    break;
                                }
                                case "days_remaining_widget": {
                                    getScheduleExams(group.isEmpty() ? null : group, (title, query) -> {
                                        try {
                                            addShortcut(
                                                    app.id,
                                                    new JSONObject().put("label", title).put("query", query).toString(),
                                                    "regular"
                                            );
                                        } catch (Exception e) {
                                            log.exception(e);
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    });
                                    break;
                                }
                            }
                        }));
                        content.addView(item);
                    }
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initShortcuts() {
        thread.runOnUI(() -> {
            log.v(TAG, "initShortcuts");
            try {
                // Переключаем режим отображения
                toggleMode(true);
                // Устанавливаем заголовок
                ImageView header_icon = fragment.container().findViewById(R.id.header_icon);
                TextView header_text = fragment.container().findViewById(R.id.header_text);
                ImageView header_close = fragment.container().findViewById(R.id.header_close);
                if (header_icon != null) {
                    header_icon.setImageResource(R.drawable.ic_shortcut);
                }
                if (header_text != null) {
                    header_text.setText(R.string.homescreen_shortcuts);
                }
                if (header_close != null) {
                    header_close.setOnClickListener(view -> route(PICK));
                }
                // Отображаем ярлыки
                ViewGroup content = fragment.container().findViewById(R.id.content);
                if (content != null) {
                    content.removeAllViews();
                    for (final Shortcut shortcut : shortcuts) {
                        ViewGroup item = (ViewGroup) fragment.inflate(R.layout.layout_homescreen_shortcuts_item);
                        ((ImageView) item.findViewById(R.id.image)).setImageResource(shortcut.image);
                        ((TextView) item.findViewById(R.id.title)).setText(shortcut.title);
                        if (shortcut.desc != null) {
                            ((TextView) item.findViewById(R.id.desc)).setText(shortcut.desc);
                        } else {
                            try {
                                View view = item.findViewById(R.id.desc);
                                ((ViewGroup) view.getParent()).removeView(view);
                            } catch (Throwable e) {
                                log.exception(e);
                            }
                        }
                        item.setOnClickListener(view -> shortcutClicked(shortcut, "regular"));
                        if ("offline".equals(shortcut.id) || ("room101".equals(shortcut.id) && "create".equals(shortcut.meta))) {
                            item.findViewById(R.id.separator).setVisibility(View.GONE);
                            item.findViewById(R.id.offline).setVisibility(View.GONE);
                        } else {
                            item.findViewById(R.id.offline).setOnClickListener(view -> shortcutClicked(shortcut, "offline"));
                        }
                        content.addView(item);
                    }
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }

    private void shortcutClicked(Shortcut shortcut, String mode) {
        thread.run(() -> {
            switch (shortcut.id) {
                case "offline": case "tab": case "room101": {
                    addShortcut(shortcut.id, shortcut.meta, mode);
                    break;
                }
                case "schedule_lessons":
                case "schedule_lessons_offline": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleLessons(group.isEmpty() ? null : group, (title, query) -> {
                        try {
                            addShortcut(
                                    shortcut.id,
                                    new JSONObject().put("label", title).put("query", query).toString(),
                                    mode
                            );
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    });
                    break;
                }
                case "schedule_exams": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleExams(group.isEmpty() ? null : group, (title, query) -> {
                        try {
                            addShortcut(
                                    shortcut.id,
                                    new JSONObject().put("label", title).put("query", query).toString(),
                                    mode
                            );
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    });
                    break;
                }
                case "schedule_attestations": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleAttestations(group.isEmpty() ? null : group, (title, query) -> {
                        try {
                            addShortcut(
                                    shortcut.id,
                                    new JSONObject().put("label", title).put("query", query).toString(),
                                    mode
                            );
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    });
                    break;
                }
                case "university": {
                    final ArrayList<String> labels = new ArrayList<>(Arrays.asList(activity.getString(R.string.persons),
                            activity.getString(R.string.faculties),
                            activity.getString(R.string.units),
                            activity.getString(R.string.news),
                            activity.getString(R.string.events),
                            activity.getString(R.string.ubuildings)));
                    final ArrayList<String> values = new ArrayList<>(Arrays.asList("persons",
                            "faculties",
                            "units",
                            "news",
                            "events",
                            "ubuildings"));
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
                    arrayAdapter.addAll(labels);
                    new AlertDialog.Builder(activity)
                            .setAdapter(arrayAdapter, (dialogInterface, position) -> {
                                try {
                                    String label = labels.get(position);
                                    String query = values.get(position);
                                    addShortcut(
                                            "university",
                                            new JSONObject().put("label", label).put("query", query).toString(),
                                            mode
                                    );
                                } catch (Exception e) {
                                    log.exception(e);
                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                }
                            })
                            .setNegativeButton(R.string.do_cancel, null)
                            .create().show();
                    break;
                }
            }
        });
    }
    
    private void toggleMode(final boolean hide) {
        toggleMode(hide, true);
    }
    
    private void toggleMode(final boolean hide, final boolean animate) {
        thread.runOnUI(new Runnable() {
            @Override
            public void run() {
                log.v(TAG, "toggleMode | hide=" + (hide ? "true" : "false") + " | animate=" + (animate ? "true" : "false"));
                try {
                    final ViewGroup initial_picker = fragment.container().findViewById(R.id.initial_picker);
                    final ViewGroup content_area = fragment.container().findViewById(R.id.content_area);
                    if (initial_picker != null && content_area != null) {
                        int height = initial_picker.getHeight();
                        if (hide) {
                            if (animate) {
                                initial_picker.setVisibility(View.VISIBLE);
                                initial_picker.setTranslationY(0);
                                initial_picker.animate()
                                        .setDuration(400)
                                        .translationY(-height)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                initial_picker.setTranslationY(0);
                                                initial_picker.setVisibility(View.GONE);
                                            }
                                        });
                                content_area.setVisibility(View.VISIBLE);
                                content_area.setTranslationY(0);
                                content_area.setAlpha(0.0f);
                                content_area.animate()
                                        .setDuration(400)
                                        .translationY(-height)
                                        .alpha(1.0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                content_area.setTranslationY(0);
                                                content_area.setAlpha(1.0f);
                                                content_area.setVisibility(View.VISIBLE);
                                            }
                                        });
                            } else {
                                initial_picker.setVisibility(View.GONE);
                                content_area.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (animate) {
                                initial_picker.setVisibility(View.VISIBLE);
                                initial_picker.setTranslationY(-height);
                                initial_picker.animate()
                                        .setDuration(400)
                                        .translationY(0)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                initial_picker.setTranslationY(0);
                                                initial_picker.setVisibility(View.VISIBLE);
                                            }
                                        });
                                content_area.setVisibility(View.VISIBLE);
                                content_area.setTranslationY(-height);
                                content_area.setAlpha(1.0f);
                                content_area.animate()
                                        .setDuration(400)
                                        .translationY(0)
                                        .alpha(0.0f)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                content_area.setVisibility(View.GONE);
                                                content_area.setAlpha(1.0f);
                                                content_area.setTranslationY(0);
                                            }
                                        });
                            } else {
                                initial_picker.setVisibility(View.VISIBLE);
                                content_area.setVisibility(View.GONE);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.exception(e);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    
    private void showWidgetsHolder() {
        thread.runOnUI(() -> {
            log.v(TAG, "showWidgetsHolder");
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.pin_app_widget_not_supported)
                    .setPositiveButton(R.string.close, null)
                    .create().show();
        });
    }

    private void getScheduleLessons(final String scope, final Result callback) {
        getSchedule(scope, callback, (context, query, handler) -> scheduleLessons.search(context, handler, query));
    }
    
    private void getScheduleExams(final String scope, final Result callback) {
        getSchedule(scope, callback, (context, query, handler) -> scheduleExams.search(context, handler, query));
    }
    
    private void getScheduleAttestations(final String scope, final Result callback) {
        getSchedule(scope, callback, (context, query, handler) -> scheduleAttestations.search(context, handler, query));
    }
    
    private void getSchedule(final String scope, final Result callback, final Schedule.ScheduleSearchProvider scheduleSearchProvider) {
        thread.run(() -> {
            try {
                log.v(TAG, "getSchedule | scope=" + scope);
                final ViewGroup layout = (ViewGroup) fragment.inflate(R.layout.widget_configure_schedule_lessons_create_search);
                final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setView(layout)
                        .setNegativeButton(R.string.do_cancel, null)
                        .create();
                final AutoCompleteTextView search_text_view = layout.findViewById(R.id.search_text_view);
                final ViewGroup search_action = layout.findViewById(R.id.search_action);
                final ViewGroup search_loading = layout.findViewById(R.id.search_loading);
                final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
                if (scope != null) {
                    search_text_view.setText(scope);
                }
                teacherPickerAdapter.setNotifyOnChange(true);
                search_text_view.setAdapter(teacherPickerAdapter);
                search_text_view.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void afterTextChanged(Editable editable) {
                        thread.run(() -> {
                            teacherPickerAdapter.clear();
                            search_text_view.dismissDropDown();
                        });
                    }
                });
                search_action.setOnClickListener(view -> thread.run(() -> {
                    final String query = search_text_view.getText().toString().trim();
                    log.v(TAG, "getSchedule | search action | clicked | query=" + query);
                    if (!query.isEmpty()) {
                        scheduleSearchProvider.onSearch(activity, query, new Schedule.Handler() {
                            @Override
                            public void onSuccess(final JSONObject json, final boolean fromCache) {
                                log.v(TAG, "getSchedule | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                                thread.run(() -> {
                                    search_loading.setVisibility(View.GONE);
                                    search_action.setVisibility(View.VISIBLE);
                                    if (json == null) {
                                        notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                                    } else {
                                        try {
                                            final String type = json.getString("type");
                                            final String query = json.getString("query");
                                            log.v(TAG, "getSchedule | search action | onSuccess | type=" + type);
                                            switch (type) {
                                                case "group": case "room": case "teacher": {
                                                    JSONArray schedule = json.getJSONArray("schedule");
                                                    String title = json.getString("title");
                                                    if (type.equals("room")) {
                                                        title = activity.getString(R.string.room) + " " + title;
                                                    }
                                                    log.v(TAG, "getSchedule | search action | onSuccess | done | query=" + query + " | title=" + title);
                                                    if (schedule.length() > 0) {
                                                        if (alertDialog.isShowing()) {
                                                            alertDialog.cancel();
                                                        }
                                                        callback.done(title, query);
                                                    } else {
                                                        notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                                                    }
                                                    break;
                                                }
                                                case "teachers": {
                                                    teacherPickerAdapter.clear();
                                                    final JSONArray teachers = json.getJSONArray("schedule");
                                                    log.v(TAG, "getSchedule | search action | onSuccess | type=" + type + " | length=" + teachers.length());
                                                    if (teachers.length() == 0) {
                                                        notificationMessage.toast(activity, activity.getString(R.string.no_teachers));
                                                    } else if (teachers.length() == 1) {
                                                        JSONObject teacher = teachers.getJSONObject(0);
                                                        if (teacher != null) {
                                                            String pid = teacher.getString("pid");
                                                            String title = teacher.getString("person");
                                                            log.v(TAG, "getSchedule | search action | onSuccess | done | query=" + pid + " | title=" + title);
                                                            if (alertDialog.isShowing()) {
                                                                alertDialog.cancel();
                                                            }
                                                            callback.done(title, pid);
                                                        } else {
                                                            notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                                                        }
                                                    } else {
                                                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                        for (int i = 0; i < teachers.length(); i++) {
                                                            arrayList.add(teachers.getJSONObject(i));
                                                        }
                                                        teacherPickerAdapter.addAll(arrayList);
                                                        teacherPickerAdapter.addTeachers(arrayList);
                                                        if (arrayList.size() > 0) {
                                                            search_text_view.showDropDown();
                                                        }
                                                    }
                                                    break;
                                                }
                                                default: {
                                                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.exception(e);
                                            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onFailure(int state) {
                                this.onFailure(0, null, state);
                            }
                            @Override
                            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                                thread.run(() -> {
                                    log.v(TAG, "getSchedule | search action | onFailure | state=" + state);
                                    search_loading.setVisibility(View.GONE);
                                    search_action.setVisibility(View.VISIBLE);
                                    notificationMessage.toast(activity, state == Client.FAILED_SERVER_ERROR ? Client.getFailureMessage(activity, statusCode) : activity.getString(R.string.schedule_not_found));
                                });
                            }
                            @Override
                            public void onProgress(final int state) {
                                thread.run(() -> {
                                    log.v(TAG, "getSchedule | search action | onProgress | state=" + state);
                                    search_loading.setVisibility(View.VISIBLE);
                                    search_action.setVisibility(View.GONE);
                                });
                            }
                            @Override
                            public void onNewRequest(Client.Request request) {
                                requestHandle = request;
                            }
                            @Override
                            public void onCancelRequest() {
                                if (requestHandle != null) {
                                    requestHandle.cancel();
                                }
                            }
                        });
                    }
                }));
                search_text_view.setOnItemClickListener((parent, view, position, id) -> thread.run(() -> {
                    try {
                        log.v(TAG, "getSchedule | search list selected");
                        final JSONObject teacher = teacherPickerAdapter.getItem(position);
                        if (teacher != null) {
                            final String query = teacher.getString("pid");
                            final String title = teacher.getString("person");
                            log.v(TAG, "getSchedule | search list selected | query=" + query + " | title=" + title);
                            if (alertDialog.isShowing()) {
                                alertDialog.cancel();
                            }
                            callback.done(title, query);
                        } else {
                            notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                        }
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                    }
                }));
                alertDialog.show();
                search_action.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }

    private void addShortcut(final String type, final String data, final String mode) {
        thread.run(() -> {
            log.v(TAG, "addShortcut | type=" + type + " | data=" + data);
            Intent intent = new Intent(ShortcutReceiver.ACTION_ADD_SHORTCUT);
            intent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
            intent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
            intent.putExtra(ShortcutReceiver.EXTRA_MODE, mode);
            activity.sendBroadcast(intent);
        });
    }
}
