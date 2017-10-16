package com.bukhmastov.cdoitmo.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.ScheduleExams;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.receivers.ShortcutReceiver;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class HomeScreenInteractionFragment extends ConnectedFragment {

    private static final String TAG = "ShortcutCreateFragment";
    private final ShortcutReceiver receiver = new ShortcutReceiver();
    private Client.Request requestHandle = null;
    private interface result {
        void done(String label, String query);
    }
    private enum MODE {PICK, WIDGETS, APPS, SHORTCUTS}
    private class App {
        private String id;
        private String title;
        private String desc;
        private String desc_extra;
        private @DrawableRes int image;
        private App(String id, String title, String desc, String desc_extra, int image) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.desc_extra = desc_extra;
            this.image = image;
        }
    }
    private class Shortcut {
        private String id;
        private String meta;
        private String title;
        private String desc;
        private @DrawableRes int image;
        private Shortcut(String id, String meta, String title, String desc, int image) {
            this.id = id;
            this.meta = meta;
            this.title = title;
            this.desc = desc;
            this.image = image;
        }
    }
    private ArrayList<App> apps = new ArrayList<>();
    private ArrayList<Shortcut> shortcuts = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        Data data = getData(activity, this.getClass());
        if (data != null) {
            activity.updateToolbar(data.title, data.image);
        }
        // Инициализируем приложения
        apps.clear();
        apps.add(new App("time_remaining_widget", activity.getString(R.string.time_remaining_widget), activity.getString(R.string.time_remaining_widget_desc), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_time_remaining_widget));
        apps.add(new App("days_remaining_widget", activity.getString(R.string.days_remaining_widget), activity.getString(R.string.days_remaining_widget_desc), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_days_remaining_widget));
        // Инициализируем ярлыки
        shortcuts.clear();
        shortcuts.add(new Shortcut("tab", "e_journal", activity.getString(R.string.e_journal), null, R.mipmap.ic_shortcut_e_journal));
        shortcuts.add(new Shortcut("tab", "protocol_changes", activity.getString(R.string.protocol_changes), null, R.mipmap.ic_shortcut_protocol_changes));
        shortcuts.add(new Shortcut("tab", "rating", activity.getString(R.string.rating), null, R.mipmap.ic_shortcut_rating));
        shortcuts.add(new Shortcut("tab", "room101", activity.getString(R.string.room101), null, R.mipmap.ic_shortcut_room101));
        shortcuts.add(new Shortcut("room101", "create", activity.getString(R.string.room101create), null, R.mipmap.ic_shortcut_room101_add));
        shortcuts.add(new Shortcut("schedule_lessons", null, activity.getString(R.string.schedule_lessons), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_schedule_lessons));
        shortcuts.add(new Shortcut("schedule_exams", null, activity.getString(R.string.schedule_exams), activity.getString(R.string.need_to_choose_schedule), R.mipmap.ic_shortcut_schedule_exams));
        shortcuts.add(new Shortcut("university", null, activity.getString(R.string.university), activity.getString(R.string.need_to_choose_type), R.mipmap.ic_shortcut_university));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homescreen_interaction, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initPicker(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Fragment resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            activity.registerReceiver(receiver, new IntentFilter(ShortcutReceiver.ACTION_INSTALL_SHORTCUT));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "Fragment paused");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            activity.unregisterReceiver(receiver);
        }
    }

    private void route(final MODE mode) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "route | mode=" + mode.toString());
                switch (mode) {
                    case PICK: initPicker(false); break;
                    case WIDGETS: initWidgets(); break;
                    case APPS: initApps(); break;
                    case SHORTCUTS: initShortcuts(); break;
                }
            }
        });
    }

    private void initPicker(final boolean first_launch) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "initPicker | first_launch=" + (first_launch ? "true" : "false"));
                try {
                    // Переключаем режим отображения
                    toggleMode(false, !first_launch);
                    // Инициализируем кнопки
                    ViewGroup menu_widgets = activity.findViewById(R.id.menu_widgets);
                    ViewGroup menu_apps = activity.findViewById(R.id.menu_apps);
                    ViewGroup menu_shortcuts = activity.findViewById(R.id.menu_shortcuts);
                    if (menu_widgets != null) {
                        menu_widgets.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                route(MODE.WIDGETS);
                            }
                        });
                    }
                    if (menu_apps != null) {
                        menu_apps.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                route(MODE.APPS);
                            }
                        });
                    }
                    if (menu_shortcuts != null) {
                        menu_shortcuts.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                route(MODE.SHORTCUTS);
                            }
                        });
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void initWidgets() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "initWidgets");
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
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void initApps() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "initApps");
                try {
                    // Переключаем режим отображения
                    toggleMode(true);
                    // Устанавливаем заголовок
                    ImageView header_icon = activity.findViewById(R.id.header_icon);
                    TextView header_text = activity.findViewById(R.id.header_text);
                    ImageView header_close = activity.findViewById(R.id.header_close);
                    if (header_icon != null) {
                        header_icon.setImageResource(R.drawable.ic_extension);
                    }
                    if (header_text != null) {
                        header_text.setText(R.string.homescreen_apps);
                    }
                    if (header_close != null) {
                        header_close.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                route(MODE.PICK);
                            }
                        });
                    }
                    // Отображаем приложения
                    ViewGroup content = activity.findViewById(R.id.content);
                    if (content != null) {
                        content.removeAllViews();
                        for (final App app : apps) {
                            ViewGroup item = (ViewGroup) inflate(R.layout.layout_homescreen_apps_item);
                            ((ImageView) item.findViewById(R.id.image)).setImageResource(app.image);
                            ((TextView) item.findViewById(R.id.title)).setText(app.title);
                            ((TextView) item.findViewById(R.id.desc)).setText(app.desc);
                            ((TextView) item.findViewById(R.id.desc_extra)).setText(app.desc_extra);
                            item.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Static.T.runThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String group = Storage.file.perm.get(activity, "user#group", "");
                                            switch (app.id) {
                                                case "time_remaining_widget": {
                                                    getScheduleLessons(group.isEmpty() ? null : group, new result() {
                                                        @Override
                                                        public void done(String label, String query) {
                                                            try {
                                                                addShortcut(
                                                                        app.id,
                                                                        new JSONObject().put("label", label).put("query", query).toString()
                                                                );
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    });
                                                    break;
                                                }
                                                case "days_remaining_widget": {
                                                    getScheduleExams(group.isEmpty() ? null : group, new result() {
                                                        @Override
                                                        public void done(String label, String query) {
                                                            try {
                                                                addShortcut(
                                                                        app.id,
                                                                        new JSONObject().put("label", label).put("query", query).toString()
                                                                );
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    });
                                                    break;
                                                }
                                            }
                                        }
                                    });

                                }
                            });
                            content.addView(item);
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void initShortcuts() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "initShortcuts");
                try {
                    // Переключаем режим отображения
                    toggleMode(true);
                    // Устанавливаем заголовок
                    ImageView header_icon = activity.findViewById(R.id.header_icon);
                    TextView header_text = activity.findViewById(R.id.header_text);
                    ImageView header_close = activity.findViewById(R.id.header_close);
                    if (header_icon != null) {
                        header_icon.setImageResource(R.drawable.ic_shortcut);
                    }
                    if (header_text != null) {
                        header_text.setText(R.string.homescreen_shortcuts);
                    }
                    if (header_close != null) {
                        header_close.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                route(MODE.PICK);
                            }
                        });
                    }
                    // Отображаем ярлыки
                    ViewGroup content = activity.findViewById(R.id.content);
                    if (content != null) {
                        content.removeAllViews();
                        for (final Shortcut shortcut : shortcuts) {
                            ViewGroup item = (ViewGroup) inflate(R.layout.layout_homescreen_shortcuts_item);
                            ((ImageView) item.findViewById(R.id.image)).setImageResource(shortcut.image);
                            ((TextView) item.findViewById(R.id.title)).setText(shortcut.title);
                            if (shortcut.desc != null) {
                                ((TextView) item.findViewById(R.id.desc)).setText(shortcut.desc);
                            } else {
                                try {
                                    View view = item.findViewById(R.id.desc);
                                    ((ViewGroup) view.getParent()).removeView(view);
                                } catch (Throwable e) {
                                    Static.error(e);
                                }
                            }
                            item.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Static.T.runThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            switch (shortcut.id) {
                                                case "tab": case "room101": {
                                                    addShortcut(shortcut.id, shortcut.meta);
                                                    break;
                                                }
                                                case "schedule_lessons": {
                                                    String group = Storage.file.perm.get(activity, "user#group", "");
                                                    getScheduleLessons(group.isEmpty() ? null : group, new result() {
                                                        @Override
                                                        public void done(String label, String query) {
                                                            try {
                                                                addShortcut(
                                                                        shortcut.id,
                                                                        new JSONObject().put("label", label).put("query", query).toString()
                                                                );
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    });
                                                    break;
                                                }
                                                case "schedule_exams": {
                                                    String group = Storage.file.perm.get(activity, "user#group", "");
                                                    getScheduleExams(group.isEmpty() ? null : group, new result() {
                                                        @Override
                                                        public void done(String label, String query) {
                                                            try {
                                                                addShortcut(
                                                                        shortcut.id,
                                                                        new JSONObject().put("label", label).put("query", query).toString()
                                                                );
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    });
                                                    break;
                                                }
                                                case "university": {
                                                    final ArrayList<String> labels = new ArrayList<>(Arrays.asList(new String[] {
                                                            activity.getString(R.string.persons),
                                                            activity.getString(R.string.faculties),
                                                            activity.getString(R.string.units),
                                                            activity.getString(R.string.ubuildings),
                                                            activity.getString(R.string.news),
                                                            activity.getString(R.string.events)
                                                    }));
                                                    final ArrayList<String> values = new ArrayList<>(Arrays.asList(new String[] {
                                                            "persons",
                                                            "faculties",
                                                            "units",
                                                            "ubuildings",
                                                            "news",
                                                            "events"
                                                    }));
                                                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
                                                    arrayAdapter.addAll(labels);
                                                    new AlertDialog.Builder(activity)
                                                            .setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int position) {
                                                                    try {
                                                                        String label = labels.get(position);
                                                                        String query = values.get(position);
                                                                        addShortcut(
                                                                                "university",
                                                                                new JSONObject().put("label", label).put("query", query).toString()
                                                                        );
                                                                    } catch (Exception e) {
                                                                        Static.error(e);
                                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                                    }
                                                                }
                                                            })
                                                            .setNegativeButton(R.string.do_cancel, null)
                                                            .create().show();
                                                    break;
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                            content.addView(item);
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });

    }
    private void toggleMode(final boolean hide) {
        toggleMode(hide, true);
    }
    private void toggleMode(final boolean hide, final boolean animate) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "toggleMode | hide=" + (hide ? "true" : "false") + " | animate=" + (animate ? "true" : "false"));
                try {
                    final ViewGroup initial_picker = activity.findViewById(R.id.initial_picker);
                    final ViewGroup content_area = activity.findViewById(R.id.content_area);
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
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void showWidgetsHolder() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "showWidgetsHolder");
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.pin_app_widget_not_supported)
                        .setPositiveButton(R.string.close, null)
                        .create().show();
            }
        });
    }

    private void getScheduleLessons(final String scope, final result callback) {
        Log.v(TAG, "getScheduleLessons | scope=" + scope);
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ViewGroup layout = (ViewGroup) inflate(R.layout.layout_widget_schedule_lessons_create_search);
                    final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                            .setView(layout)
                            .setNegativeButton(R.string.do_cancel, null)
                            .create();
                    final AutoCompleteTextView search_text_view = layout.findViewById(R.id.search_text_view);
                    final ViewGroup search_action = layout.findViewById(R.id.search_action);
                    final ViewGroup search_loading = layout.findViewById(R.id.search_loading);
                    final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<JSONObject>());
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
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    teacherPickerAdapter.clear();
                                    search_text_view.dismissDropDown();
                                }
                            });
                        }
                    });
                    search_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    final String query = search_text_view.getText().toString().trim();
                                    Log.v(TAG, "getScheduleLessons | search action | clicked | query=" + query);
                                    if (!query.isEmpty()) {
                                        if (requestHandle != null) {
                                            requestHandle.cancel();
                                        }
                                        ScheduleLessons scheduleLessons = new ScheduleLessons(activity);
                                        scheduleLessons.setHandler(new ScheduleLessons.response() {
                                            @Override
                                            public void onNewRequest(Client.Request request) {
                                                requestHandle = request;
                                            }
                                            @Override
                                            public void onProgress(final int state) {
                                                Log.v(TAG, "getScheduleLessons | search action | onProgress | state=" + state);
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.VISIBLE);
                                                        search_action.setVisibility(View.GONE);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onFailure(final int state) {
                                                Log.v(TAG, "getScheduleLessons | search action | onFailure | state=" + state);
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.GONE);
                                                        search_action.setVisibility(View.VISIBLE);
                                                        Static.snackBar(activity, state == IfmoRestClient.FAILED_SERVER_ERROR ? IfmoRestClient.getFailureMessage(activity, -1) : activity.getString(R.string.schedule_not_found));
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onSuccess(final JSONObject json) {
                                                Log.v(TAG, "getScheduleLessons | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.GONE);
                                                        search_action.setVisibility(View.VISIBLE);
                                                        if (json == null) {
                                                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                                        } else {
                                                            try {
                                                                String type = json.getString("type");
                                                                String query = json.getString("query");
                                                                Log.v(TAG, "getScheduleLessons | search action | onSuccess | type=" + type);
                                                                switch (type) {
                                                                    case "group": case "room": case "teacher": {
                                                                        String label = json.getString("label");
                                                                        if (Objects.equals(type, "room")) {
                                                                            label = activity.getString(R.string.room) + " " + label;
                                                                        }
                                                                        Log.v(TAG, "getScheduleLessons | search action | onSuccess | done | query=" + query + " | label=" + label);
                                                                        if (alertDialog != null && alertDialog.isShowing()) {
                                                                            alertDialog.cancel();
                                                                        }
                                                                        callback.done(label, query);
                                                                        break;
                                                                    }
                                                                    case "teacher_picker": {
                                                                        teacherPickerAdapter.clear();
                                                                        JSONArray list = json.getJSONArray("list");
                                                                        Log.v(TAG, "getScheduleLessons | search action | onSuccess | type=" + type + " | length=" + list.length());
                                                                        if (list.length() == 1) {
                                                                            JSONObject item = list.getJSONObject(0);
                                                                            if (item != null) {
                                                                                String pid = item.getString("pid");
                                                                                String label = item.getString("person");
                                                                                Log.v(TAG, "getScheduleLessons | search action | onSuccess | done | query=" + pid + " | label=" + label);
                                                                                if (alertDialog.isShowing()) {
                                                                                    alertDialog.cancel();
                                                                                }
                                                                                callback.done(label, pid);
                                                                            } else {
                                                                                Static.snackBar(activity, getString(R.string.something_went_wrong));
                                                                            }
                                                                        } else {
                                                                            ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                                            for (int i = 0; i < list.length(); i++) {
                                                                                arrayList.add(list.getJSONObject(i));
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
                                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                                        break;
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                        scheduleLessons.search(query);
                                    }
                                }
                            });
                        }
                    });
                    search_text_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.v(TAG, "getScheduleLessons | search list selected");
                                        JSONObject item = teacherPickerAdapter.getItem(position);
                                        if (item != null) {
                                            String query = item.getString("pid");
                                            String label = item.getString("person");
                                            Log.v(TAG, "getScheduleLessons | search list selected | query=" + query + " | label=" + label);
                                            if (alertDialog.isShowing()) {
                                                alertDialog.cancel();
                                            }
                                            callback.done(label, query);
                                        } else {
                                            Static.snackBar(activity, getString(R.string.something_went_wrong));
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                    alertDialog.show();
                    search_action.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }
    private void getScheduleExams(final String scope, final result callback) {
        Log.v(TAG, "getScheduleExams | scope=" + scope);
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ViewGroup layout = (ViewGroup) inflate(R.layout.layout_widget_schedule_lessons_create_search);
                    final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                            .setView(layout)
                            .setNegativeButton(R.string.do_cancel, null)
                            .create();
                    final AutoCompleteTextView search_text_view = layout.findViewById(R.id.search_text_view);
                    final ViewGroup search_action = layout.findViewById(R.id.search_action);
                    final ViewGroup search_loading = layout.findViewById(R.id.search_loading);
                    final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<JSONObject>());
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
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    teacherPickerAdapter.clear();
                                    search_text_view.dismissDropDown();
                                }
                            });
                        }
                    });
                    search_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    final String query = search_text_view.getText().toString().trim();
                                    Log.v(TAG, "getScheduleExams | search action | clicked | query=" + query);
                                    if (!query.isEmpty()) {
                                        if (requestHandle != null) {
                                            requestHandle.cancel();
                                        }
                                        ScheduleExams scheduleExams = new ScheduleExams(activity);
                                        scheduleExams.setHandler(new ScheduleExams.response() {
                                            @Override
                                            public void onNewRequest(Client.Request request) {
                                                requestHandle = request;
                                            }
                                            @Override
                                            public void onProgress(int state) {
                                                Log.v(TAG, "getScheduleExams | search action | onProgress | state=" + state);
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.VISIBLE);
                                                        search_action.setVisibility(View.GONE);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onFailure(final int state) {
                                                Log.v(TAG, "getScheduleExams | search action | onFailure | state=" + state);
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.GONE);
                                                        search_action.setVisibility(View.VISIBLE);
                                                        Static.snackBar(activity, state == IfmoClient.FAILED_SERVER_ERROR ? IfmoClient.getFailureMessage(activity, -1) : activity.getString(R.string.schedule_not_found));
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onSuccess(final JSONObject json) {
                                                Log.v(TAG, "getScheduleExams | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        search_loading.setVisibility(View.GONE);
                                                        search_action.setVisibility(View.VISIBLE);
                                                        if (json == null) {
                                                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                                        } else {
                                                            try {
                                                                String type = json.getString("type");
                                                                Log.v(TAG, "getScheduleExams | search action | onSuccess | type=" + type);
                                                                switch (type) {
                                                                    case "group": case "teacher": {
                                                                        JSONArray schedule = json.getJSONArray("schedule");
                                                                        String query = json.getString("scope");
                                                                        String label = json.getString("scope");
                                                                        Log.v(TAG, "getScheduleExams | search action | onSuccess | done | query=" + query + " | label=" + label);
                                                                        if (schedule.length() > 0) {
                                                                            if (alertDialog.isShowing()) {
                                                                                alertDialog.cancel();
                                                                            }
                                                                            callback.done(label, query);
                                                                        } else {
                                                                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                                                        }
                                                                        break;
                                                                    }
                                                                    case "teacher_picker": {
                                                                        teacherPickerAdapter.clear();
                                                                        JSONArray teachers = json.getJSONArray("teachers");
                                                                        if (teachers.length() > 0) {
                                                                            if (teachers.length() == 1) {
                                                                                JSONObject teacher = teachers.getJSONObject(0);
                                                                                String query = teacher.getString("scope");
                                                                                String label = teacher.getString("name");
                                                                                Log.v(TAG, "getScheduleExams | search action | onSuccess | done | query=" + query + " | label=" + label);
                                                                                if (alertDialog.isShowing()) {
                                                                                    alertDialog.cancel();
                                                                                }
                                                                                callback.done(label, query);
                                                                            } else {
                                                                                ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                                                for (int i = 0; i < teachers.length(); i++) {
                                                                                    JSONObject teacher = teachers.getJSONObject(i);
                                                                                    JSONObject teacherAdapted = new JSONObject();
                                                                                    teacherAdapted.put("person", teacher.getString("name"));
                                                                                    teacherAdapted.put("pid", teacher.getString("scope"));
                                                                                    teacherAdapted.put("post", "");
                                                                                    arrayList.add(teacherAdapted);
                                                                                }
                                                                                teacherPickerAdapter.addAll(arrayList);
                                                                                teacherPickerAdapter.addTeachers(arrayList);
                                                                                if (arrayList.size() > 0) {
                                                                                    search_text_view.showDropDown();
                                                                                }
                                                                            }
                                                                        } else {
                                                                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                                                        }
                                                                        break;
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                Static.error(e);
                                                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                        scheduleExams.search(query);
                                    }
                                }
                            });
                        }
                    });
                    search_text_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.v(TAG, "getScheduleExams | search list selected");
                                        JSONObject item = teacherPickerAdapter.getItem(position);
                                        if (item != null) {
                                            String query = item.getString("pid");
                                            String label = item.getString("person");
                                            Log.v(TAG, "getScheduleExams | search list selected | query=" + query + " | label=" + label);
                                            if (alertDialog.isShowing()) {
                                                alertDialog.cancel();
                                            }
                                            callback.done(label, query);
                                        } else {
                                            Static.snackBar(activity, getString(R.string.something_went_wrong));
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                    alertDialog.show();
                    search_action.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Static.error(e);
                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            }
        });
    }

    private void addShortcut(final String type, final String data) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "addShortcut | type=" + type + " | data=" + data);
                Intent intent = new Intent(ShortcutReceiver.ACTION_ADD_SHORTCUT);
                intent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
                intent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    new ShortcutReceiver().onReceive(activity, intent);
                } else {
                    activity.sendBroadcast(intent);
                }
            }
        });
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
