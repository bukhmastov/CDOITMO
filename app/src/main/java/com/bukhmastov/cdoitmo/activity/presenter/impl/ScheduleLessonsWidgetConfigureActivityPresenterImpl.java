package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.activity.presenter.ScheduleLessonsWidgetConfigureActivityPresenter;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.dialog.ColorPickerDialog;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidget;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidgetStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class ScheduleLessonsWidgetConfigureActivityPresenterImpl implements ScheduleLessonsWidgetConfigureActivityPresenter {

    private static final String TAG = "SLWidgetConfigureActivity";
    private ScheduleLessonsWidgetConfigureActivity activity = null;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean isDarkTheme = false;
    private Client.Request requestHandle = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    Storage storage;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Theme theme;
    @Inject
    ScheduleLessonsWidgetStorage scheduleLessonsWidgetStorage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleLessonsWidgetConfigureActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);

    }

    @Override
    public void setActivity(@NonNull ScheduleLessonsWidgetConfigureActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.i(TAG, "Activity created");
        firebaseAnalyticsProvider.logCurrentScreen(activity);
        activity.setResult(Activity.RESULT_CANCELED);
        final String th = theme.getAppTheme(activity);
        isDarkTheme = "dark".equals(th) || "black".equals(th);
        Toolbar toolbar = activity.findViewById(R.id.toolbar_widget);
        if (toolbar != null) {
            theme.applyToolbarTheme(activity, toolbar);
            toolbar.setTitle(R.string.configure_schedule_widget);
            activity.setSupportActionBar(toolbar);
        }
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.w(TAG, "Wrong AppWidgetId provided by intent's extra");
            close(Activity.RESULT_CANCELED, null);
            return;
        }
        Settings.Schedule.query = Default.Schedule.query;
        Settings.Schedule.title = Default.Schedule.title;
        init();
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
    }

    private void init() {
        initPartPreview();
        initPartSchedule();
        initPartTheme();
        initPartUpdate();
        initPartDynamicShift();
        initFinishButton();
    }
    
    private void initPartPreview() {
        log.v(TAG, "initPartPreview");
        thread.run(() -> {
            // Starting from Android 27 (8.1) there is no longer free access to current wallpaper
            // Getting wallpaper requires "dangerous" permission android.permission.READ_EXTERNAL_STORAGE
            // To avoid using this permission, we just not gonna use wallpaper for widget preview
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                try {
                    final WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
                    if (wallpaperManager == null) {
                        throw new NullPointerException("WallpaperManager is null");
                    }
                    final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                    if (wallpaperDrawable == null) {
                        throw new NullPointerException("WallpaperDrawable is null");
                    }
                    thread.runOnUI(() -> {
                        ImageView part_preview_background = activity.findViewById(R.id.part_preview_background);
                        if (part_preview_background != null) {
                            part_preview_background.setImageDrawable(wallpaperDrawable);
                        }
                    });
                } catch (Exception ignore) {
                    // just ignore
                }
            }
            Settings.Theme.background = isDarkTheme ? Default.Theme.Dark.background : Default.Theme.Light.background;
            Settings.Theme.text       = isDarkTheme ? Default.Theme.Dark.text       : Default.Theme.Light.text;
            updateDemo();
        });
    }
    
    private void initPartSchedule() {
        log.v(TAG, "initPartSchedule");
        thread.run(() -> {
            try {
                ViewGroup part_schedule = activity.findViewById(R.id.part_schedule);
                part_schedule.setOnClickListener(view -> {
                    if (Settings.Schedule.query.isEmpty()) {
                        String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group");
                        if (group.isEmpty()) {
                            // TODO uncomment, when personal schedule will be ready
                            activatePartSchedule(/*"mine"*/);
                        } else {
                            activatePartSchedule(group);
                        }
                    } else {
                        activatePartSchedule(Settings.Schedule.title);
                    }
                });
                updateScheduleSummary();
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initPartTheme() {
        log.v(TAG, "initPartTheme");
        thread.run(() -> {
            try {
                ViewGroup part_theme = activity.findViewById(R.id.part_theme);
                part_theme.setOnClickListener(view -> activatePartTheme());
                updateThemeSummary();
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initPartUpdate() {
        log.v(TAG, "initPartUpdate");
        thread.run(() -> {
            try {
                ViewGroup part_update = activity.findViewById(R.id.part_update);
                part_update.setOnClickListener(view -> activatePartUpdate());
                updateUpdateSummary();
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initPartDynamicShift() {
        log.v(TAG, "initPartDynamicShift");
        thread.runOnUI(() -> {
            try {
                ViewGroup part_dynamic_shift = activity.findViewById(R.id.part_automatic_shift);
                Switch part_dynamic_shift_switch = activity.findViewById(R.id.part_automatic_shift_switch);
                part_dynamic_shift_switch.setChecked(Default.useShiftAutomatic);
                part_dynamic_shift_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    activatePartDynamicShift(isChecked);
                });
                part_dynamic_shift.setOnClickListener(view -> {
                    part_dynamic_shift_switch.setChecked(!part_dynamic_shift_switch.isChecked());
                });
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void initFinishButton() {
        log.v(TAG, "initFinishButton");
        thread.run(() -> {
            try {
                Button add_button = activity.findViewById(R.id.add_button);
                add_button.setOnClickListener(view -> activateFinish());
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }

    private void activatePartSchedule() {
        activatePartSchedule(null);
    }
    
    private void activatePartSchedule(final String title) {
        log.v(TAG, "activatePartSchedule | scope=" + title);
        thread.run(() -> {
            try {
                final ViewGroup layout = (ViewGroup) inflate(R.layout.widget_configure_schedule_lessons_create_search);
                final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setView(layout)
                        .setNegativeButton(R.string.do_cancel, null)
                        .create();
                final AutoCompleteTextView search_text_view = layout.findViewById(R.id.search_text_view);
                final ViewGroup search_action = layout.findViewById(R.id.search_action);
                final ViewGroup search_loading = layout.findViewById(R.id.search_loading);
                final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
                if (title != null) {
                    search_text_view.setText(title);
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
                    log.v(TAG, "activatePartSchedule | search action | clicked | query=" + query);
                    if (!query.isEmpty()) {
                        scheduleLessons.search(activity, new Schedule.Handler() {
                            @Override
                            public void onSuccess(final JSONObject json, final boolean fromCache) {
                                thread.run(() -> {
                                    log.v(TAG, "activatePartSchedule | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                                    search_loading.setVisibility(View.GONE);
                                    search_action.setVisibility(View.VISIBLE);
                                    if (json == null) {
                                        notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                    } else {
                                        try {
                                            final String type = json.getString("type");
                                            final String query1 = json.getString("query");
                                            log.v(TAG, "activatePartSchedule | search action | onSuccess | type=" + type);
                                            switch (type) {
                                                case "group": case "room": case "teacher": {
                                                    Settings.Schedule.query = query1;
                                                    switch (type) {
                                                        case "group": case "teacher": {
                                                            Settings.Schedule.title = json.getString("title");
                                                            break;
                                                        }
                                                        case "room": {
                                                            Settings.Schedule.title = activity.getString(R.string.room) + " " + json.getString("title");
                                                            break;
                                                        }
                                                    }
                                                    log.v(TAG, "activatePartSchedule | search action | onSuccess | done | query=" + Settings.Schedule.query + " | title=" + Settings.Schedule.title);
                                                    if (alertDialog != null && alertDialog.isShowing()) {
                                                        alertDialog.cancel();
                                                    }
                                                    updateScheduleSummary();
                                                    break;
                                                }
                                                case "teachers": {
                                                    teacherPickerAdapter.clear();
                                                    final JSONArray schedule = json.getJSONArray("schedule");
                                                    log.v(TAG, "activatePartSchedule | search action | onSuccess | type=" + type + " | length=" + schedule.length());
                                                    if (schedule.length() == 1) {
                                                        JSONObject item = schedule.getJSONObject(0);
                                                        if (item != null) {
                                                            Settings.Schedule.query = item.getString("pid");
                                                            Settings.Schedule.title = item.getString("person");
                                                            log.v(TAG, "activatePartSchedule | search action | onSuccess | done | query=" + Settings.Schedule.query + " | title=" + Settings.Schedule.title);
                                                            if (alertDialog.isShowing()) {
                                                                alertDialog.cancel();
                                                            }
                                                            updateScheduleSummary();
                                                        } else {
                                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                        }
                                                    } else {
                                                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                        for (int i = 0; i < schedule.length(); i++) {
                                                            arrayList.add(schedule.getJSONObject(i));
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
                                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.exception(e);
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
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
                                log.v(TAG, "activatePartSchedule | search action | onFailure | state=" + state + " | statusCode=" + statusCode);
                                thread.run(() -> {
                                    search_loading.setVisibility(View.GONE);
                                    search_action.setVisibility(View.VISIBLE);
                                    String text = activity.getString(R.string.schedule_not_found);
                                    switch (state) {
                                        case Client.FAILED_OFFLINE: text = activity.getString(R.string.offline_mode_on); break;
                                        case Client.FAILED_TRY_AGAIN: text = activity.getString(R.string.load_failed); break;
                                        case Client.FAILED_SERVER_ERROR: text = Client.getFailureMessage(activity, statusCode); break;
                                        case Client.FAILED_CORRUPTED_JSON: text = activity.getString(R.string.server_provided_corrupted_json); break;
                                    }
                                    notificationMessage.snackBar(activity, text);
                                });
                            }
                            @Override
                            public void onProgress(final int state) {
                                log.v(TAG, "activatePartSchedule | search action | onProgress | state=" + state);
                                thread.run(() -> {
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
                        }, query);
                    }
                }));
                search_text_view.setOnItemClickListener((parent, view, position, id) -> thread.run(() -> {
                    try {
                        log.v(TAG, "activatePartSchedule | search list selected");
                        JSONObject item = teacherPickerAdapter.getItem(position);
                        if (item != null) {
                            Settings.Schedule.query = item.getString("pid");
                            Settings.Schedule.title = item.getString("person");
                            log.v(TAG, "activatePartSchedule | search list selected | query=" + Settings.Schedule.query + " | title=" + Settings.Schedule.title);
                            if (alertDialog.isShowing()) {
                                alertDialog.cancel();
                            }
                            updateScheduleSummary();
                        } else {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                alertDialog.show();
                search_action.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void activatePartTheme() {
        log.v(TAG, "activatePartTheme");
        thread.run(() -> {
            try {
                // define variables
                final ViewGroup layout = (ViewGroup) inflate(R.layout.widget_configure_schedule_lessons_create_theme);

                final ViewGroup default_theme_light = layout.findViewById(R.id.default_theme_light);
                final TextView default_theme_light_background = layout.findViewById(R.id.default_theme_light_background);
                final TextView default_theme_light_text = layout.findViewById(R.id.default_theme_light_text);
                final TextView default_theme_light_opacity = layout.findViewById(R.id.default_theme_light_opacity);

                final ViewGroup default_theme_dark = layout.findViewById(R.id.default_theme_dark);
                final TextView default_theme_dark_background = layout.findViewById(R.id.default_theme_dark_background);
                final TextView default_theme_dark_text = layout.findViewById(R.id.default_theme_dark_text);
                final TextView default_theme_dark_opacity = layout.findViewById(R.id.default_theme_dark_opacity);

                final ViewGroup background_color_picker = layout.findViewById(R.id.background_color_picker);
                final ImageView background_color_picker_image = layout.findViewById(R.id.background_color_picker_image);
                final TextView background_color_picker_header = layout.findViewById(R.id.background_color_picker_header);
                final TextView background_color_picker_value = layout.findViewById(R.id.background_color_picker_value);
                final TextView background_color_picker_hint = layout.findViewById(R.id.background_color_picker_hint);

                final ViewGroup text_color_picker = layout.findViewById(R.id.text_color_picker);
                final ImageView text_color_picker_image = layout.findViewById(R.id.text_color_picker_image);
                final TextView text_color_picker_header = layout.findViewById(R.id.text_color_picker_header);
                final TextView text_color_picker_value = layout.findViewById(R.id.text_color_picker_value);
                final TextView text_color_picker_hint = layout.findViewById(R.id.text_color_picker_hint);

                final ViewGroup background_opacity_picker = layout.findViewById(R.id.background_opacity_picker);
                final SeekBar background_opacity_picker_seek_bar = layout.findViewById(R.id.background_opacity_picker_seek_bar);

                // setup ui

                final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setView(layout)
                        .setPositiveButton(R.string.apply, (dialogInterface, i) -> thread.run(() -> {
                            log.v(TAG, "activatePartTheme | apply");
                            try {
                                String background = background_color_picker_value.getText().toString().trim();
                                if (background.charAt(0) != '#') {
                                    throw new Exception();
                                }
                                Color.parseColor(background);
                                Settings.Theme.background = background;
                            } catch (Exception ignore) {
                                // just ignore
                            }
                            try {
                                String text = text_color_picker_value.getText().toString().trim();
                                if (text.charAt(0) != '#') {
                                    throw new Exception();
                                }
                                Color.parseColor(text);
                                Settings.Theme.text = text;
                            } catch (Exception ignore) {
                                // just ignore
                            }
                            try {
                                int opacity = background_opacity_picker_seek_bar.getProgress();
                                if (opacity >= 0 && opacity <= 255) {
                                    Settings.Theme.opacity = opacity;
                                }
                            } catch (Exception ignore) {
                                // just ignore
                            }
                            log.v(TAG, "activatePartTheme | apply | background=" + Settings.Theme.background + " | text=" + Settings.Theme.text + " | opacity=" + Settings.Theme.opacity);
                            updateDemo();
                            updateThemeSummary();
                        }))
                        .setNegativeButton(R.string.do_cancel, null)
                        .create();

                default_theme_light.setOnClickListener(view -> thread.run(() -> {
                    log.v(TAG, "activatePartTheme | light theme selected");
                    try {
                        Settings.Theme.text       = Default.Theme.Light.text;
                        Settings.Theme.background = Default.Theme.Light.background;
                        Settings.Theme.opacity    = Default.Theme.Light.opacity;
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                        updateDemo();
                        updateThemeSummary();
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                default_theme_light_background.setText(activity.getString(R.string.background_color) + ": " + Default.Theme.Light.background);
                default_theme_light_text.setText(activity.getString(R.string.text_color) + ": " + Default.Theme.Light.text);
                default_theme_light_opacity.setText(activity.getString(R.string.background_opacity) + ": " + Default.Theme.Light.opacity);

                default_theme_dark.setOnClickListener(view -> thread.run(() -> {
                    log.v(TAG, "activatePartTheme | dark theme selected");
                    try {
                        Settings.Theme.text       = Default.Theme.Dark.text;
                        Settings.Theme.background = Default.Theme.Dark.background;
                        Settings.Theme.opacity    = Default.Theme.Dark.opacity;
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                        updateDemo();
                        updateThemeSummary();
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                default_theme_dark_background.setText(activity.getString(R.string.background_color) + ": " + Default.Theme.Dark.background);
                default_theme_dark_text.setText(activity.getString(R.string.text_color) + ": " + Default.Theme.Dark.text);
                default_theme_dark_opacity.setText(activity.getString(R.string.background_opacity) + ": " + Default.Theme.Dark.opacity);

                background_color_picker.setOnClickListener(view -> thread.run(() -> {
                    log.v(TAG, "activatePartTheme | background color picker clicked");
                    new ColorPickerDialog(activity, new ColorPickerDialog.ColorPickerCallback() {
                        @Override
                        public void result(final String hex) {
                            thread.run(() -> {
                                log.v(TAG, "activatePartTheme | background color picker | hex=" + hex);
                                applyColor(hex, background_color_picker, background_color_picker_image, background_color_picker_value, background_color_picker_header, background_color_picker_hint);
                            });
                        }
                        @Override
                        public void exception(Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }).show(Settings.Theme.background);
                }));
                applyColor(Settings.Theme.background, background_color_picker, background_color_picker_image, background_color_picker_value, background_color_picker_header, background_color_picker_hint);

                text_color_picker.setOnClickListener(view -> thread.run(() -> {
                    log.v(TAG, "activatePartTheme | text color picker clicked");
                    new ColorPickerDialog(activity, new ColorPickerDialog.ColorPickerCallback() {
                        @Override
                        public void result(final String hex) {
                            thread.run(() -> {
                                log.v(TAG, "activatePartTheme | text color picker | hex=" + hex);
                                applyColor(hex, text_color_picker, text_color_picker_image, text_color_picker_value, text_color_picker_header, text_color_picker_hint);
                            });
                        }
                        @Override
                        public void exception(Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }).show(Settings.Theme.text);
                }));
                applyColor(Settings.Theme.text, text_color_picker, text_color_picker_image, text_color_picker_value, text_color_picker_header, text_color_picker_hint);

                background_opacity_picker.getBackground().setAlpha((int) ((double) (255 - Settings.Theme.opacity) * 0.5));
                background_opacity_picker_seek_bar.setProgress(Settings.Theme.opacity);
                background_opacity_picker_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                        try {
                            if (progress < 0) {
                                progress = 0;
                            }
                            if (progress > 255) {
                                progress = 255;
                            }
                            background_opacity_picker.getBackground().setAlpha((int) ((double) (255 - progress) * 0.5));
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                alertDialog.show();
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void activatePartUpdate() {
        log.v(TAG, "activatePartUpdate");
        thread.run(() -> {
            try {
                int select = 0;
                switch (Settings.updateTime){
                    case 0: select = 0; break;
                    case 12: select = 1; break;
                    case 24: select = 2; break;
                    case 168: select = 3; break;
                    case 672: select = 4; break;
                }
                final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setTitle(R.string.update_interval)
                        .setSingleChoiceItems(R.array.pref_widget_refresh_titles, select, (dialog, which) -> {
                            log.v(TAG, "activatePartUpdate | apply");
                            thread.run(() -> {
                                switch (which){
                                    case 0: Settings.updateTime = 0; break;
                                    case 1: Settings.updateTime = 12; break;
                                    case 2: Settings.updateTime = 24; break;
                                    case 3: Settings.updateTime = 168; break;
                                    case 4: Settings.updateTime = 672; break;
                                }
                                log.v(TAG, "activatePartUpdate | apply | which=" + which + " | updateTime=" + Settings.updateTime);
                                updateUpdateSummary();
                                dialog.dismiss();
                            });
                        })
                        .setNegativeButton(R.string.do_cancel, null)
                        .create();
                alertDialog.show();
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void activatePartDynamicShift(boolean checked) {
        log.v(TAG, "activatePartDynamicShift | checked=", checked);
        thread.run(() -> {
            try {
                Settings.useShiftAutomatic = checked;
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void activateFinish() {
        log.v(TAG, "activateFinish");
        thread.run(() -> {
            try {
                if (Settings.Schedule.query == null || Settings.Schedule.query.trim().isEmpty()) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.need_to_choose_schedule));
                    return;
                }
                JSONObject theme = new JSONObject();
                theme.put("background", Settings.Theme.background);
                theme.put("text", Settings.Theme.text);
                theme.put("opacity", Settings.Theme.opacity);
                JSONObject settings = new JSONObject();
                settings.put("query", Settings.Schedule.query);
                settings.put("theme", theme);
                settings.put("updateTime", Settings.updateTime);
                settings.put("shift", 0);
                settings.put("shiftAutomatic", 0);
                settings.put("useShiftAutomatic", Settings.useShiftAutomatic);
                log.v(TAG, "activateFinish | settings=" + settings.toString());
                scheduleLessonsWidgetStorage.save(activity, mAppWidgetId, "settings", settings.toString());
                (new ScheduleLessonsWidget()).updateAppWidget(activity, AppWidgetManager.getInstance(activity), mAppWidgetId, false);
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                close(Activity.RESULT_OK, resultValue);
                firebaseAnalyticsProvider.logEvent(
                        activity,
                        FirebaseAnalyticsProvider.Event.WIDGET_INSTALL,
                        firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.WIDGET_QUERY, Settings.Schedule.query)
                );
            } catch (Exception e) {
                log.w(TAG, "activateFinish | failed to create widget");
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.failed_to_create_widget));
            }
        });
    }

    private void updateDemo() {
        log.v(TAG, "updateDemo");
        thread.runOnUI(() -> {
            try {
                int background = parseColor(Settings.Theme.background, Settings.Theme.opacity);
                int text = parseColor(Settings.Theme.text);
                ViewGroup widget_content = activity.findViewById(R.id.widget_content);
                if (widget_content != null) {
                    ViewGroup widget_header = widget_content.findViewById(R.id.widget_header);
                    TextView widget_title = widget_content.findViewById(R.id.widget_title);
                    TextView widget_day_title = widget_content.findViewById(R.id.widget_day_title);
                    ImageView widget_refresh_button = widget_content.findViewById(R.id.widget_refresh_button);
                    ImageView widget_controls_open_button = widget_content.findViewById(R.id.widget_controls_open_button);
                    TextView slw_item_time_start = widget_content.findViewById(R.id.slw_item_time_start);
                    ImageView slw_item_time_icon = widget_content.findViewById(R.id.slw_item_time_icon);
                    TextView slw_item_time_end = widget_content.findViewById(R.id.slw_item_time_end);
                    TextView slw_item_title = widget_content.findViewById(R.id.slw_item_title);
                    TextView slw_item_desc = widget_content.findViewById(R.id.slw_item_desc);
                    TextView slw_item_meta = widget_content.findViewById(R.id.slw_item_meta);
                    widget_content.setBackgroundColor(background);
                    widget_header.setBackgroundColor(background);
                    widget_title.setTextColor(text);
                    widget_day_title.setTextColor(text);
                    widget_refresh_button.setImageTintList(ColorStateList.valueOf(text));
                    widget_controls_open_button.setImageTintList(ColorStateList.valueOf(text));
                    slw_item_time_start.setTextColor(text);
                    slw_item_time_icon.setImageTintList(ColorStateList.valueOf(text));
                    slw_item_time_end.setTextColor(text);
                    slw_item_title.setTextColor(text);
                    slw_item_desc.setTextColor(text);
                    slw_item_meta.setTextColor(text);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }
    
    private void updateScheduleSummary() {
        log.v(TAG, "updateScheduleSummary");
        thread.runOnUI(() -> {
            try {
                TextView part_schedule_summary = activity.findViewById(R.id.part_schedule_summary);
                part_schedule_summary.setText(!Settings.Schedule.query.isEmpty() ? Settings.Schedule.title : activity.getString(R.string.need_to_choose_schedule));
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void updateThemeSummary() {
        log.v(TAG, "updateThemeSummary");
        thread.runOnUI(() -> {
            try {
                TextView part_theme_summary = activity.findViewById(R.id.part_theme_summary);
                String summary;
                if (Settings.Theme.opacity == Default.Theme.Light.opacity && Settings.Theme.background.toUpperCase().equals(Default.Theme.Light.background) && Settings.Theme.text.toUpperCase().equals(Default.Theme.Light.text)) {
                    summary = activity.getString(R.string.pref_light_theme);
                } else if (Settings.Theme.opacity == Default.Theme.Dark.opacity && Settings.Theme.background.toUpperCase().equals(Default.Theme.Dark.background) && Settings.Theme.text.toUpperCase().equals(Default.Theme.Dark.text)) {
                    summary = activity.getString(R.string.pref_dark_theme);
                } else {
                    summary  = "Фон" + ": " + Settings.Theme.background + ", ";
                    summary += "Текст" + ": " + Settings.Theme.text + ", ";
                    summary += "Прозрачность" + ": " + Settings.Theme.opacity;
                }
                part_theme_summary.setText(summary);
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    
    private void updateUpdateSummary() {
        log.v(TAG, "updateUpdateSummary");
        thread.runOnUI(() -> {
            try {
                TextView part_update_summary = activity.findViewById(R.id.part_update_summary);
                String summary;
                switch (Settings.updateTime){
                    case 0: summary = activity.getString(R.string.manually); break;
                    case 12: summary = activity.getString(R.string.once_per_12_hours); break;
                    case 24: summary = activity.getString(R.string.once_per_1_day); break;
                    case 168: summary = activity.getString(R.string.once_per_1_week); break;
                    case 672: summary = activity.getString(R.string.once_per_4_weeks); break;
                    default: summary = activity.getString(R.string.unknown); break;
                }
                part_update_summary.setText(summary);
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }

    private void applyColor(String hex, ViewGroup picker, ImageView image, TextView value, TextView header, TextView hint) {
        ColorStateList highlight = ColorStateList.valueOf(Color.parseColor(hex) > Color.parseColor("#757575") ? Color.BLACK : Color.WHITE);
        value.setText(hex);
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP) {
            picker.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hex)));
            image.setImageTintList(highlight);
            header.setTextColor(highlight);
            value.setTextColor(highlight);
            hint.setTextColor(highlight);
        }
    }

    private int parseColor(String color) {
        return parseColor(color, 255);
    }
    
    private int parseColor(String color, int opacity) {
        log.v(TAG, "parseColor | color=" + color + " | opacity=" + opacity);
        int parsed = Color.parseColor(color);
        return Color.argb(opacity, Color.red(parsed), Color.green(parsed), Color.blue(parsed));
    }

    private void close(int result, Intent intent) {
        log.v(TAG, "close | result=" + result);
        if (intent == null){
            activity.setResult(result);
        } else {
            activity.setResult(result, intent);
        }
        if (requestHandle != null) requestHandle.cancel();
        activity.finish();
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
