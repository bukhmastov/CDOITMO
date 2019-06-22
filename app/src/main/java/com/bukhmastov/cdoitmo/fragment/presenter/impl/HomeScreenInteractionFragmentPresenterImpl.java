package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.array.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.HomeScreenInteractionFragmentPresenter;
import com.bukhmastov.cdoitmo.function.BiConsumer;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.entity.ShortcutQuery;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
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
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidget;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import static com.bukhmastov.cdoitmo.util.Thread.AHS;

public class HomeScreenInteractionFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements HomeScreenInteractionFragmentPresenter {

    private static final String TAG = "ShortcutCreateFragment";
    private ShortcutReceiver receiver = new ShortcutReceiver();
    private AutoCompleteTextView searchTextView = null;
    private TeacherPickerAdapter teacherPickerAdapter = null;
    private AlertDialog alertDialog = null;

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
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(AHS);
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            ConnectedFragment.Data data = ConnectedFragment.getData(activity, fragment.getClass());
            activity.updateToolbar(activity, data.title, data.image);
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
            shortcuts.add(new Shortcut("tab", "groups", activity.getString(R.string.study_groups), null, R.mipmap.ic_shortcut_groups));
            shortcuts.add(new Shortcut("tab", "scholarship", activity.getString(R.string.scholarship), null, R.mipmap.ic_shortcut_scholarship));
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ShortcutReceiver.ACTION_ADD_SHORTCUT);
            filter.addAction(ShortcutReceiver.ACTION_SHORTCUT_INSTALLED);
            filter.addAction(ShortcutReceiver.ACTION_INSTALL_SHORTCUT);
            activity.registerReceiver(receiver, filter);
        });
    }

    @Override
    public void onPause() {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "Fragment paused");
            activity.unregisterReceiver(receiver);
        });
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        initPicker(true);
    }

    private void route(@MODE String mode) {
        thread.run(AHS, () -> {
            log.v(TAG, "route | mode=", mode);
            switch (mode) {
                case PICK: initPicker(false); break;
                case WIDGETS: initWidgets(); break;
                case APPS: initApps(); break;
                case SHORTCUTS: initShortcuts(); break;
            }
        });
    }

    private void initPicker(boolean isFirstLaunch) {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "initPicker | isFirstLaunch=", isFirstLaunch);
            // Переключаем режим отображения
            toggleMode(false, !isFirstLaunch);
            // Инициализируем кнопки
            ViewGroup widgets = fragment.container().findViewById(R.id.menu_widgets);
            ViewGroup apps = fragment.container().findViewById(R.id.menu_apps);
            ViewGroup shortcuts = fragment.container().findViewById(R.id.menu_shortcuts);
            if (widgets != null) {
                widgets.setOnClickListener(view -> route(WIDGETS));
            }
            if (apps != null) {
                apps.setOnClickListener(view -> route(APPS));
            }
            if (shortcuts != null) {
                shortcuts.setOnClickListener(view -> route(SHORTCUTS));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void initWidgets() {
        thread.run(AHS, () -> {
            log.v(TAG, "initWidgets");
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                showWidgetsHolder();
                return;
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
            if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
                showWidgetsHolder();
                return;
            }
            ComponentName componentName = new ComponentName(activity, ScheduleLessonsWidget.class);
            Intent intent = new Intent(activity, ScheduleLessonsWidget.class);
            intent.setAction(ScheduleLessonsWidget.ACTION_WIDGET_OPEN_CONFIGURATION);
            PendingIntent onSuccess = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (!appWidgetManager.requestPinAppWidget(componentName, null, onSuccess)) {
                showWidgetsHolder();
            }
        }, throwable -> {
            log.exception(throwable);
            showWidgetsHolder();
        });
    }
    
    private void initApps() {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "initApps");
            // Переключаем режим отображения
            toggleMode(true);
            // Устанавливаем заголовок
            ImageView headerIcon = fragment.container().findViewById(R.id.header_icon);
            TextView headerText = fragment.container().findViewById(R.id.header_text);
            ImageView headerClose = fragment.container().findViewById(R.id.header_close);
            if (headerIcon != null) {
                headerIcon.setImageResource(R.drawable.ic_extension);
            }
            if (headerText != null) {
                headerText.setText(R.string.homescreen_apps);
            }
            if (headerClose != null) {
                headerClose.setOnClickListener(view -> route(PICK));
            }
            // Отображаем приложения
            ViewGroup content = fragment.container().findViewById(R.id.content);
            if (content == null) {
                return;
            }
            content.removeAllViews();
            for (App app : apps) {
                ViewGroup item = (ViewGroup) fragment.inflate(R.layout.layout_homescreen_apps_item);
                ((ImageView) item.findViewById(R.id.image)).setImageResource(app.image);
                ((TextView) item.findViewById(R.id.title)).setText(app.title);
                ((TextView) item.findViewById(R.id.desc)).setText(app.desc);
                ((TextView) item.findViewById(R.id.desc_extra)).setText(app.desc_extra);
                item.setOnClickListener(view -> thread.run(AHS, () -> {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    switch (app.id) {
                        case "time_remaining_widget": {
                            getScheduleLessons(group.isEmpty() ? null : group, (title, query) -> {
                                thread.standalone(() -> {
                                    addShortcut(
                                            app.id, "regular",
                                            new ShortcutQuery(query, title).toJsonString()
                                    );
                                }, throwable -> {
                                    log.exception(throwable);
                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                });
                            });
                            break;
                        }
                        case "days_remaining_widget": {
                            getScheduleExams(group.isEmpty() ? null : group, (title, query) -> {
                                thread.standalone(() -> {
                                    addShortcut(
                                            app.id, "regular",
                                            new ShortcutQuery(query, title).toJsonString()
                                    );
                                }, throwable -> {
                                    log.exception(throwable);
                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                });
                            });
                            break;
                        }
                    }
                }));
                content.addView(item);
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void initShortcuts() {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "initShortcuts");
            // Переключаем режим отображения
            toggleMode(true);
            // Устанавливаем заголовок
            ImageView headerIcon = fragment.container().findViewById(R.id.header_icon);
            TextView headerText = fragment.container().findViewById(R.id.header_text);
            ImageView headerClose = fragment.container().findViewById(R.id.header_close);
            if (headerIcon != null) {
                headerIcon.setImageResource(R.drawable.ic_shortcut);
            }
            if (headerText != null) {
                headerText.setText(R.string.homescreen_shortcuts);
            }
            if (headerClose != null) {
                headerClose.setOnClickListener(view -> route(PICK));
            }
            // Отображаем ярлыки
            ViewGroup content = fragment.container().findViewById(R.id.content);
            if (content == null) {
                return;
            }
            content.removeAllViews();
            for (Shortcut shortcut : shortcuts) {
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
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void shortcutClicked(Shortcut shortcut, String mode) {
        thread.run(AHS, () -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            switch (shortcut.id) {
                case "offline": case "tab": case "room101": {
                    thread.standalone(() -> {
                        addShortcut(shortcut.id, mode, shortcut.meta);
                    });
                    break;
                }
                case "schedule_lessons":
                case "schedule_lessons_offline": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleLessons(group.isEmpty() ? null : group, (title, query) -> {
                        thread.standalone(() -> {
                            addShortcut(
                                    shortcut.id, mode,
                                    new ShortcutQuery(query, title).toJsonString()
                            );
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        });
                    });
                    break;
                }
                case "schedule_exams": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleExams(group.isEmpty() ? null : group, (title, query) -> {
                        thread.standalone(() -> {
                            addShortcut(
                                    shortcut.id, mode,
                                    new ShortcutQuery(query, title).toJsonString()
                            );
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        });
                    });
                    break;
                }
                case "schedule_attestations": {
                    String group = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#group", "");
                    getScheduleAttestations(group.isEmpty() ? null : group, (title, query) -> {
                        thread.standalone(() -> {
                            addShortcut(
                                    shortcut.id, mode,
                                    new ShortcutQuery(query, title).toJsonString()
                            );
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        });
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
                                thread.standalone(() -> {
                                    String label = labels.get(position);
                                    String query = values.get(position);
                                    addShortcut(
                                            "university", mode,
                                            new ShortcutQuery(query, label).toJsonString()
                                    );
                                }, throwable -> {
                                    log.exception(throwable);
                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                });
                            })
                            .setNegativeButton(R.string.do_cancel, null)
                            .create().show();
                    break;
                }
            }
        });
    }

    private void toggleMode(boolean hide) {
        toggleMode(hide, true);
    }
    
    private void toggleMode(boolean hide, boolean animate) {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "toggleMode | hide=", hide, " | animate=", animate);
            ViewGroup initialPicker = fragment.container().findViewById(R.id.initial_picker);
            ViewGroup contentArea = fragment.container().findViewById(R.id.content_area);
            if (initialPicker != null && contentArea != null) {
                int height = initialPicker.getHeight();
                if (hide) {
                    if (animate) {
                        initialPicker.setVisibility(View.VISIBLE);
                        initialPicker.setTranslationY(0);
                        initialPicker.animate()
                                .setDuration(400)
                                .translationY(-height)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        initialPicker.setTranslationY(0);
                                        initialPicker.setVisibility(View.GONE);
                                    }
                                });
                        contentArea.setVisibility(View.VISIBLE);
                        contentArea.setTranslationY(0);
                        contentArea.setAlpha(0.0f);
                        contentArea.animate()
                                .setDuration(400)
                                .translationY(-height)
                                .alpha(1.0f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        contentArea.setTranslationY(0);
                                        contentArea.setAlpha(1.0f);
                                        contentArea.setVisibility(View.VISIBLE);
                                    }
                                });
                    } else {
                        initialPicker.setVisibility(View.GONE);
                        contentArea.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (animate) {
                        initialPicker.setVisibility(View.VISIBLE);
                        initialPicker.setTranslationY(-height);
                        initialPicker.animate()
                                .setDuration(400)
                                .translationY(0)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        initialPicker.setTranslationY(0);
                                        initialPicker.setVisibility(View.VISIBLE);
                                    }
                                });
                        contentArea.setVisibility(View.VISIBLE);
                        contentArea.setTranslationY(-height);
                        contentArea.setAlpha(1.0f);
                        contentArea.animate()
                                .setDuration(400)
                                .translationY(0)
                                .alpha(0.0f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        contentArea.setVisibility(View.GONE);
                                        contentArea.setAlpha(1.0f);
                                        contentArea.setTranslationY(0);
                                    }
                                });
                    } else {
                        initialPicker.setVisibility(View.VISIBLE);
                        contentArea.setVisibility(View.GONE);
                    }
                }
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void showWidgetsHolder() {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "showWidgetsHolder");
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.pin_app_widget_not_supported)
                    .setPositiveButton(R.string.close, null)
                    .create().show();
        });
    }

    private void getScheduleLessons(String scope, BiConsumer<String, String> callback) {
        getSchedule(scope, callback, (Schedule.ScheduleSearchProvider<SLessons>) (query, handler) -> {
            scheduleLessons.search(query, handler);
        }, schedule -> {
            log.v(TAG, "getScheduleLessons | onSuccess | type=", schedule.getType());
            switch (schedule.getType()) {
                case "group": case "room": case "teacher": {
                    if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                        notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                        return;
                    }
                    String query = schedule.getQuery();
                    String title = ("room".equals(schedule.getType()) ? activity.getString(R.string.room) + " " : "") + schedule.getTitle();
                    log.v(TAG, "getScheduleLessons | onSuccess | done | query=", query, " | title=", title);
                    if (alertDialog.isShowing()) {
                        alertDialog.cancel();
                    }
                    callback.accept(title, query);
                    break;
                }
                case "teachers": {
                    teacherPickerAdapter.clear();
                    if (schedule.getTeachers() == null || CollectionUtils.isEmpty(schedule.getTeachers().getTeachers())) {
                        notificationMessage.toast(activity, activity.getString(R.string.no_teachers));
                        return;
                    }
                    ArrayList<STeacher> teachers = schedule.getTeachers().getTeachers();
                    log.v(TAG, "getScheduleLessons | onSuccess | type=", schedule.getType(), " | length=", teachers.size());
                    if (teachers.size() == 1) {
                        STeacher teacher = teachers.get(0);
                        if (teacher == null) {
                            notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                            return;
                        }
                        String query = teacher.getPersonId();
                        String title = teacher.getPerson();
                        log.v(TAG, "getScheduleLessons | onSuccess | done | query=", query, " | title=", title);
                        if (alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                        callback.accept(title, query);
                        return;
                    }
                    thread.runOnUI(AHS, () -> {
                        teacherPickerAdapter.addAll(teachers);
                        teacherPickerAdapter.addTeachers(teachers);
                        if (teachers.size() > 0) {
                            searchTextView.showDropDown();
                        }
                    });
                    break;
                }
                default: {
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    break;
                }
            }
        });
    }
    
    private void getScheduleExams(String scope, BiConsumer<String, String> callback) {
        getSchedule(scope, callback, (Schedule.ScheduleSearchProvider<SExams>) (query, handler) -> {
            scheduleExams.search(query, handler);
        }, schedule -> {
            log.v(TAG, "getScheduleExams | onSuccess | type=", schedule.getType());
            switch (schedule.getType()) {
                case "group": case "teacher": {
                    if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                        notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                        return;
                    }
                    String query = schedule.getQuery();
                    String title = schedule.getTitle();
                    log.v(TAG, "getScheduleExams | onSuccess | done | query=", query, " | title=", title);
                    if (alertDialog.isShowing()) {
                        alertDialog.cancel();
                    }
                    callback.accept(title, query);
                    break;
                }
                case "teachers": {
                    teacherPickerAdapter.clear();
                    if (schedule.getTeachers() == null || CollectionUtils.isEmpty(schedule.getTeachers().getTeachers())) {
                        notificationMessage.toast(activity, activity.getString(R.string.no_teachers));
                        return;
                    }
                    ArrayList<STeacher> teachers = schedule.getTeachers().getTeachers();
                    log.v(TAG, "getScheduleExams | onSuccess | type=", schedule.getType(), " | length=", teachers.size());
                    if (teachers.size() == 1) {
                        STeacher teacher = teachers.get(0);
                        if (teacher == null) {
                            notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                            return;
                        }
                        String query = teacher.getPersonId();
                        String title = teacher.getPerson();
                        log.v(TAG, "getScheduleExams | onSuccess | done | query=", query, " | title=", title);
                        if (alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                        callback.accept(title, query);
                        return;
                    }
                    thread.runOnUI(AHS, () -> {
                        teacherPickerAdapter.addAll(teachers);
                        teacherPickerAdapter.addTeachers(teachers);
                        if (teachers.size() > 0) {
                            searchTextView.showDropDown();
                        }
                    });
                    break;
                }
                default: {
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    break;
                }
            }
        });
    }
    
    private void getScheduleAttestations(String scope, BiConsumer<String, String> callback) {
        getSchedule(scope, callback, (Schedule.ScheduleSearchProvider<SAttestations>) (query, handler) -> {
            scheduleAttestations.search(query, handler);
        }, schedule -> {
            log.v(TAG, "getScheduleAttestations | onSuccess | type=", schedule.getType());
            switch (schedule.getType()) {
                case "group": {
                    if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                        notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                        return;
                    }
                    String query = schedule.getQuery();
                    String title = schedule.getTitle();
                    log.v(TAG, "getScheduleAttestations | onSuccess | done | query=", query, " | title=", title);
                    if (alertDialog.isShowing()) {
                        alertDialog.cancel();
                    }
                    callback.accept(title, query);
                    break;
                }
                default: {
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    break;
                }
            }
        });
    }
    
    private <T extends ScheduleJsonEntity> void getSchedule(
            String scope, BiConsumer<String, String> callback,
            Schedule.ScheduleSearchProvider<T> scheduleSearchProvider,
            Consumer<T> onSuccess
    ) {
        thread.runOnUI(AHS, () -> {
            log.v(TAG, "getSchedule | scope=", scope);
            ViewGroup layout = (ViewGroup) fragment.inflate(R.layout.widget_configure_schedule_lessons_create_search);
            searchTextView = layout.findViewById(R.id.search_text_view);
            teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
            alertDialog = new AlertDialog.Builder(activity)
                    .setView(layout)
                    .setNegativeButton(R.string.do_cancel, null)
                    .create();
            ViewGroup searchAction = layout.findViewById(R.id.search_action);
            ViewGroup searchLoading = layout.findViewById(R.id.search_loading);
            if (scope != null) {
                searchTextView.setText(scope);
            }
            teacherPickerAdapter.setNotifyOnChange(true);
            searchTextView.setAdapter(teacherPickerAdapter);
            searchTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void afterTextChanged(Editable editable) {
                    thread.runOnUI(AHS, () -> {
                        teacherPickerAdapter.clear();
                        searchTextView.dismissDropDown();
                    });
                }
            });
            searchAction.setOnClickListener(view -> thread.run(AHS, () -> {
                String query = searchTextView.getText().toString().trim();
                log.v(TAG, "getSchedule | search action | clicked | query=", query);
                if (StringUtils.isBlank(query)) {
                    return;
                }
                scheduleSearchProvider.onSearch(query, new Schedule.Handler<T>() {
                    @Override
                    public void onSuccess(T schedule, boolean fromCache) {
                        thread.runOnUI(AHS, () -> {
                            log.v(TAG, "getSchedule | search action | onSuccess | schedule=", (schedule == null ? "null" : "notnull"));
                            searchLoading.setVisibility(View.GONE);
                            searchAction.setVisibility(View.VISIBLE);
                        });
                        if (schedule == null) {
                            notificationMessage.toast(activity, activity.getString(R.string.schedule_not_found));
                        }
                        onSuccess.accept(schedule);
                    }
                    @Override
                    public void onFailure(int code, Client.Headers headers, int state) {
                        thread.runOnUI(AHS, () -> {
                            log.v(TAG, "getSchedule | search action | onFailure | state=", state);
                            searchLoading.setVisibility(View.GONE);
                            searchAction.setVisibility(View.VISIBLE);
                            notificationMessage.toast(activity, scheduleLessons.getFailedMessage(code, state));
                        });
                    }
                    @Override
                    public void onProgress(int state) {
                        thread.runOnUI(AHS, () -> {
                            log.v(TAG, "getSchedule | search action | onProgress | state=", state);
                            searchLoading.setVisibility(View.VISIBLE);
                            searchAction.setVisibility(View.GONE);
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
            }));
            searchTextView.setOnItemClickListener((parent, view, position, id) -> {
                thread.run(AHS, () -> {
                    log.v(TAG, "getSchedule | search list selected");
                    STeacher teacher = teacherPickerAdapter.getItem(position);
                    if (teacher == null) {
                       notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                       return;
                    }
                    String query = teacher.getPersonId();
                    String title = teacher.getPerson();
                    log.v(TAG, "getSchedule | search list selected | query=", query, " | title=", title);
                    thread.runOnUI(AHS, () -> {
                        if (alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                    });
                    callback.accept(title, query);
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.toast(activity, fragment.getString(R.string.something_went_wrong));
                });
            });
            alertDialog.show();
            searchAction.setVisibility(View.VISIBLE);
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void addShortcut(String type, String mode, String data) {
        log.v(TAG, "addShortcut | type=", type, " | data=", data);
        Intent intent = new Intent(ShortcutReceiver.ACTION_ADD_SHORTCUT);
        intent.putExtra(ShortcutReceiver.EXTRA_TYPE, type);
        intent.putExtra(ShortcutReceiver.EXTRA_MODE, mode);
        intent.putExtra(ShortcutReceiver.EXTRA_DATA, data);
        activity.sendBroadcast(intent);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getThreadToken() {
        return AHS;
    }
}
