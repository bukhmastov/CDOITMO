package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleLessonsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.model.rva.RVALessons;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

public class ScheduleLessonsTabFragmentPresenterImpl implements ScheduleLessonsTabFragmentPresenter {

    private static final String TAG = "SLTabFragment";
    private final Schedule.Handler<SLessons> scheduleHandler;
    private ConnectedActivity activity = null;
    private SLessons schedule = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private View container = null;
    private boolean invalidate = false;
    private boolean invalidateRefresh = false;
    private int type = ScheduleLessonsTabHostFragmentPresenter.DEFAULT_INVALID_TYPE;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;
    @Inject
    ScheduleLessonsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;

    public ScheduleLessonsTabFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
        scheduleHandler = new Schedule.Handler<SLessons>() {
            @Override
            public void onSuccess(SLessons schedule, boolean fromCache) {
                thread.run(() -> {
                    ScheduleLessonsTabFragmentPresenterImpl.this.schedule = schedule;
                    if ("teachers".equals(schedule.getType()) && schedule.getTeachers() != null &&
                        CollectionUtils.isNotEmpty(schedule.getTeachers().getTeachers()) &&
                        schedule.getTeachers().getTeachers().size() == 1
                    ) {
                        tabHostPresenter.setQuery(schedule.getTeachers().getTeachers().get(0).getPersonId());
                        tabHostPresenter.invalidate(false);
                        return;
                    }
                    int weekday = time.getWeek(activity);
                    String reducedLessonMode = storagePref.get(activity, "pref_schedule_lessons_view_of_reduced_lesson", "compact");
                    ScheduleLessonsRVA adapter = new ScheduleLessonsRVA(activity, schedule, type, weekday, reducedLessonMode);
                    adapter.setClickListener(R.id.schedule_lessons_menu, ScheduleLessonsTabFragmentPresenterImpl.this::lessonsMenu);
                    adapter.setClickListener(R.id.lesson_touch_icon, ScheduleLessonsTabFragmentPresenterImpl.this::lessonMenu);
                    adapter.setClickListener(R.id.teacher_picker_item, ScheduleLessonsTabFragmentPresenterImpl.this::teacherSelected);
                    thread.runOnUI(() -> {
                        draw(activity, R.layout.layout_schedule_both_recycle_list);
                        // prepare
                        SwipeRefreshLayout swipe = container.findViewById(R.id.schedule_swipe);
                        RecyclerView recyclerView = container.findViewById(R.id.schedule_list);
                        if (swipe == null || recyclerView == null) {
                            return;
                        }
                        LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        // swipe
                        swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                        swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                        swipe.setOnRefreshListener(() -> {
                            swipe.setRefreshing(false);
                            tabHostPresenter.invalidate(true);
                        });
                        // recycle view (list)
                        recyclerView.setLayoutManager(layoutManager);
                        recyclerView.setAdapter(adapter);
                        recyclerView.setHasFixedSize(true);
                        // scroll to prev position listener (only android 23+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            recyclerView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                                View v = recyclerView.getChildAt(0);
                                int offset = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());
                                int position = layoutManager.findFirstVisibleItemPosition();
                                ScheduleLessonsTabHostFragmentPresenter.Scroll scroll = tabHostPresenter.scroll().get(type, null);
                                if (scroll == null) {
                                    scroll = new ScheduleLessonsTabHostFragmentPresenter.Scroll();
                                }
                                scroll.position = position;
                                scroll.offset = offset;
                                tabHostPresenter.scroll().put(type, scroll);
                            });
                        }
                        // scroll to prev position OR to today's schedule
                        ScheduleLessonsTabHostFragmentPresenter.Scroll scroll = tabHostPresenter.scroll().get(type, null);
                        if (scroll != null) {
                            layoutManager.scrollToPositionWithOffset(scroll.position, scroll.offset);
                        } else if (storagePref.get(activity, "pref_schedule_lessons_scroll_to_day", true)) {
                            int position = -1;
                            switch (time.getCalendar().get(Calendar.DAY_OF_WEEK)) {
                                case Calendar.MONDAY: position = adapter.getDayPosition(0); if (position >= 0) break;
                                case Calendar.TUESDAY: position = adapter.getDayPosition(1); if (position >= 0) break;
                                case Calendar.WEDNESDAY: position = adapter.getDayPosition(2); if (position >= 0) break;
                                case Calendar.THURSDAY: position = adapter.getDayPosition(3); if (position >= 0) break;
                                case Calendar.FRIDAY: position = adapter.getDayPosition(4); if (position >= 0) break;
                                case Calendar.SATURDAY: position = adapter.getDayPosition(5); if (position >= 0) break;
                                case Calendar.SUNDAY: position = adapter.getDayPosition(6); if (position >= 0) break;
                            }
                            if (position >= 0) {
                                layoutManager.scrollToPosition(position);
                            }
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        failed(activity);
                    });
                }, throwable -> {
                    log.exception(throwable);
                    failed(activity);
                });
            }
            @Override
            public void onFailure(int statusCode, Client.Headers headers, int state) {
                thread.runOnUI(() -> {
                    ScheduleLessonsTabFragmentPresenterImpl.this.schedule = null;
                    log.v(TAG, "onFailure | statusCode=", statusCode, " | state=", state);
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case Schedule.FAILED_OFFLINE: {
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_offline_text);
                            if (view != null) {
                                view.findViewById(R.id.offline_reload).setOnClickListener(v -> load(false));
                                draw(view);
                            }
                            break;
                        }
                        case Client.FAILED_TRY_AGAIN:
                        case Client.FAILED_SERVER_ERROR:
                        case Client.FAILED_CORRUPTED_JSON:
                        case Schedule.FAILED_LOAD: {
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
                            if (view != null) {
                                switch (state) {
                                    case Client.FAILED_SERVER_ERROR:
                                        ((TextView) view.findViewById(R.id.try_again_message)).setText(Client.getFailureMessage(activity, statusCode));
                                        break;
                                    case Client.FAILED_CORRUPTED_JSON:
                                        ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.server_provided_corrupted_json);
                                        break;
                                }
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_EMPTY_QUERY: {
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_empty_query);
                            if (view != null) {
                                view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleLessonsSearchActivity.class)));
                                view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null));
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_NOT_FOUND: {
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_nothing_to_display_compact);
                            if (view != null) {
                                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_INVALID_QUERY: {
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_text);
                            if (view != null) {
                                ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_MINE_NEED_ISU: {
                            // TODO replace with isu auth, when isu will be ready
                            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
                            if (view != null) {
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                            }
                            break;
                        }
                    }
                }, throwable -> {
                    log.exception(throwable);
                    failed(activity);
                });
            }
            @Override
            public void onProgress(int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "onProgress | state=", state);
                    ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading_text);
                    if (view != null) {
                        ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                        draw(view);
                    }
                }, throwable -> {
                    log.exception(throwable);
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
        };
    }

    @Event
    public void onOpenIntentEventFailed(OpenIntentEvent.Failed event) {
        if (!event.getIdentity().equals(ScheduleLessonsTabFragmentPresenterImpl.class.getName())) {
            return;
        }
        notificationMessage.snackBar(activity, activity.getString(R.string.failed_to_start_geo_activity));
    }

    @Override
    public void onCreate(Bundle savedInstanceState, ConnectedActivity activity, Fragment fragment) {
        thread.run(() -> {
            this.activity = activity;
            Bundle bundle = fragment.getArguments();
            if (bundle != null && bundle.containsKey("type")) {
                type = bundle.getInt("type");
            } else {
                log.w(TAG, "onCreate | UNDEFINED TYPE, going to use DEFAULT_TYPE");
                type = ScheduleLessonsTabHostFragmentPresenter.DEFAULT_TYPE;
            }
            log.v(TAG, "Fragment created | type=", type);
            tabHostPresenter.tabs().put(type, refresh -> thread.run(() -> {
                log.v(TAG, "onInvalidate | type=", type, " | refresh=", refresh);
                if (fragment.isResumed()) {
                    invalidate = false;
                    invalidateRefresh = false;
                    load(refresh);
                } else {
                    invalidate = true;
                    invalidateRefresh = refresh;
                }
            }));
        });
    }

    @Override
    public void onDestroy() {
        thread.run(() -> {
            log.v(TAG, "Fragment destroyed | type=", type);
            tabHostPresenter.tabs().remove(type);
            tabHostPresenter.scroll().remove(type);
        });
    }

    @Override
    public void onCreateView(View container) {
        this.container = container;
    }

    @Override
    public void onDestroyView() {
        loaded = false;
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed | type=", type, " | loaded=", loaded, " | invalidate=", invalidate, " | invalidateRefresh=", invalidateRefresh);
            if (invalidate) {
                invalidate = false;
                loaded = true;
                load(invalidateRefresh);
                invalidateRefresh = false;
            } else if (!loaded) {
                loaded = true;
                load(false);
            }
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "Fragment paused | type=", type);
            if (requestHandle != null && requestHandle.cancel()) {
                log.v(TAG, "Fragment paused | type=", type, " | paused and requested reload");
                loaded = false;
            }
        });
    }

    private void load(final boolean refresh) {
        thread.runOnUI(() -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                failed(activity);
                return;
            }
            draw(activity, R.layout.state_loading_text);
            thread.run(() -> {
                if (activity == null || tabHostPresenter.getQuery() == null) {
                    log.w(TAG, "load | some values are null | activity=", activity, " | getQuery()=", tabHostPresenter.getQuery());
                    failed(activity);
                    return;
                }
                if (!tabHostPresenter.isSameQueryRequested()) {
                    tabHostPresenter.scroll().clear();
                }
                if (refresh) {
                    scheduleLessons.search(tabHostPresenter.getQuery(), 0, scheduleHandler);
                } else {
                    scheduleLessons.search(tabHostPresenter.getQuery(), scheduleHandler);
                }
            }, throwable -> {
                log.exception(throwable);
                failed(activity);
            });
        }, throwable -> {
            log.exception(throwable);
            failed(activity);
        });
    }

    private void failed(Context context) {
        try {
            if (context == null) {
                log.w(TAG, "failed | context is null");
                return;
            }
            View failed = inflate(context, R.layout.state_failed_button);
            if (failed != null) {
                failed.findViewById(R.id.try_again_reload).setOnClickListener(view -> load(false));
                draw(failed);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private void draw(View view) {
        thread.runOnUI(() -> {
            ViewGroup vg = container.findViewById(R.id.container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void draw(Context context, int layoutId) {
        try {
            draw(inflate(context, layoutId));
        } catch (Exception e){
            log.exception(e);
        }
    }

    private View inflate(Context context, @LayoutRes int layout) throws InflateException {
        if (context == null) {
            log.e(TAG, "Failed to inflate layout, context is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }

    // -->- Lessons global menu ->--

    private void lessonsMenu(View view, RVALessons entity) {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            boolean isCached = cacheToken != null && StringUtils.isNotBlank(storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, ""));
            thread.runOnUI(() -> {
                PopupMenu popup = new PopupMenu(activity, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.schedule_lessons, menu);
                menu.findItem(isCached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                popup.setOnMenuItemClickListener(menuItem -> {
                    lessonsMenuSelected(menuItem);
                    return false;
                });
                popup.show();
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void lessonsMenuSelected(MenuItem item) {
        log.v(TAG, "Lessons menu | popup item | clicked | " + item.getTitle().toString());
        switch (item.getItemId()) {
            case R.id.add_to_cache:
            case R.id.remove_from_cache: toggleCache(); break;
            case R.id.add_lesson: addLesson(); break;
            case R.id.add_military_day: addMilitaryDay(); break;
            case R.id.share_schedule: shareSchedule(); break;
            case R.id.share_changes: shareChanges(); break;
            case R.id.remove_changes: clearChanges(); break;
            case R.id.open_settings: activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null); break;
        }
    }
    
    private void toggleCache() {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            if (cacheToken == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
                if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                } else {
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                }
                return;
            }
            if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, schedule.toJsonString())) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
            } else {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
        });
    }
    
    private void addLesson() {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            if (!scheduleLessonsHelper.createLesson(activity, schedule.getQuery(), schedule.getTitle(), schedule.getType(), time.getWeekDay(), new SLesson(), null)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void addMilitaryDay() {
        thread.runOnUI(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            List<String> days = new ArrayList<>(Arrays.asList(activity.getString(R.string.monday), activity.getString(R.string.tuesday), activity.getString(R.string.wednesday), activity.getString(R.string.thursday), activity.getString(R.string.friday), activity.getString(R.string.saturday), activity.getString(R.string.sunday)));
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
            arrayAdapter.addAll(days);
            new AlertDialog.Builder(activity)
                    .setAdapter(arrayAdapter, (dialogInterface, position) -> {
                        thread.run(() -> {
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("Утренний осмотр, строевая подготовка", "Военка", 2, "9:05", "9:20"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("1 пара", "Военка", 2, "9:30", "10:50"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("2 пара", "Военка", 2, "11:00", "12:20"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("3 пара", "Военка", 2, "12:30", "13:50"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("4 пара", "Военка", 2, "14:50", "16:10"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("Строевая подготовка", "Военка", 2, "16:20", "16:35"), null);
                            scheduleLessonsHelper.createLesson(schedule.getQuery(), position, makeLesson("Кураторский час", "Военка", 2, "16:45", "17:30"), null);
                            tabHostPresenter.invalidateOnDemand();
                        }, throwable -> {
                            log.exception(throwable);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        });
                    })
                    .setNegativeButton(R.string.do_cancel, null)
                    .create().show();
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void shareSchedule() {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(scheduleLessons.getScheduleHeader(schedule.getTitle(), schedule.getType()));
            sb.append("\n");
            if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                sb.append(activity.getString(R.string.no_lessons));
            } else {
                if (shareScheduleIsScheduleHasEvenOddWeekLessons(schedule.getSchedule())) {
                    shareScheduleAppendLessonsForWeek(sb, schedule.getSchedule(), schedule.getType(), 0);
                    shareScheduleAppendLessonsForWeek(sb, schedule.getSchedule(), schedule.getType(), 1);
                } else {
                    shareScheduleAppendLessonsForWeek(sb, schedule.getSchedule(), schedule.getType(), 2);
                }
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "schedule_lessons_plain"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private boolean shareScheduleIsScheduleHasEvenOddWeekLessons(ArrayList<SDay> days) {
        for (SDay day : days) {
            if (day == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            for (SLesson lesson : day.getLessons()) {
                if (lesson != null && lesson.getParity() != 2) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void shareScheduleAppendLessonsForWeek(StringBuilder sb, ArrayList<SDay> days, String scheduleType, int week) {
        if (week == 0 || week == 1) {
            sb.append("\n");
            sb.append(activity.getString(week == 0 ? R.string.week_even : R.string.week_odd).toUpperCase());
            sb.append("\n");
        }
        int lessonsThisWeek = 0;
        for (SDay day : days) {
            if (day == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            TreeSet<SLesson> lessons = scheduleLessonsHelper.filterAndSortLessons(day.getLessons(), week, true);
            if (CollectionUtils.isEmpty(lessons)) {
                continue;
            }
            sb.append("\n");
            sb.append(getDayTitle(day));
            sb.append("\n");
            for (SLesson lesson : lessons) {
                if (lesson == null) {
                    continue;
                }
                lessonsThisWeek++;
                String subject = lesson.getSubject();
                String desc = getLessonDesc(lesson, scheduleType);
                String meta = getLessonMeta(lesson, scheduleType);
                String lessonType = lesson.getType();
                switch (lessonType) {
                    case "practice": lessonType = activity.getString(R.string.practice); break;
                    case "lecture": lessonType = activity.getString(R.string.lecture); break;
                    case "lab": lessonType = activity.getString(R.string.lab); break;
                    case "iws": lessonType = activity.getString(R.string.iws); break;
                }
                sb.append(StringUtils.isNotBlank(lesson.getTimeStart()) ? lesson.getTimeStart() : "∞");
                sb.append("-");
                sb.append(StringUtils.isNotBlank(lesson.getTimeEnd()) ? lesson.getTimeEnd() : "∞");
                sb.append(" ");
                sb.append(subject);
                sb.append(". ");
                if (StringUtils.isNotBlank(lessonType)) {
                    sb.append(lessonType).append(". ");
                }
                if (StringUtils.isNotBlank(desc)) {
                    sb.append(desc).append(". ");
                }
                if (StringUtils.isNotBlank(meta)) {
                    sb.append(meta).append(". ");
                }
                sb.append("\n");
            }
        }
        if (lessonsThisWeek == 0) {
            sb.append(activity.getString(R.string.no_lessons)).append("\n");
        }
    }
    
    private void shareChanges() {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            Bundle extras = new Bundle();
            extras.putString("action", "share");
            extras.putString("query", schedule.getQuery());
            extras.putString("type", schedule.getType());
            extras.putString("title", schedule.getTitle());
            thread.runOnUI(() -> activity.openActivityOrFragment(ScheduleLessonsShareFragment.class, extras));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void clearChanges() {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                    .setMessage(R.string.pref_schedule_lessons_clear_direct_additional_warning)
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.proceed, (dialog, which) -> thread.run(() -> {
                        if (!scheduleLessonsHelper.clearChanges(schedule.getQuery(), () -> tabHostPresenter.invalidateOnDemand())) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.no_changes));
                        }
                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    // -<-- Lessons global menu --<- || -->- Lesson menu ->--

    private void lessonMenu(View view, RVALessons entity) {
        thread.run(() -> {
            if (schedule == null || entity.getLesson() == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            SLesson lesson = entity.getLesson();
            int weekday = entity.getWeekday();
            PopupMenu popup = new PopupMenu(activity, view);
            Menu menu = popup.getMenu();
            popup.getMenuInflater().inflate(R.menu.schedule_lessons_item, menu);
            bindMenuItem(menu, R.id.open_group, StringUtils.isBlank(lesson.getGroup()) ? null : activity.getString(R.string.group) + " " + lesson.getGroup());
            bindMenuItem(menu, R.id.open_teacher, StringUtils.isBlank(lesson.getTeacherName()) || StringUtils.isBlank(lesson.getTeacherId()) ? null : lesson.getTeacherName());
            bindMenuItem(menu, R.id.open_room, StringUtils.isBlank(lesson.getRoom()) ? null : activity.getString(R.string.room) + " " + lesson.getRoom());
            bindMenuItem(menu, R.id.open_location, lesson.getBuilding());
            bindMenuItem(menu, R.id.reduce_lesson, "normal".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.reduce_lesson) : null);
            bindMenuItem(menu, R.id.restore_lesson, "reduced".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.restore_lesson) : null);
            bindMenuItem(menu, R.id.delete_lesson, "synthetic".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.delete_lesson) : null);
            bindMenuItem(menu, R.id.edit_lesson, "synthetic".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.edit_lesson) : null);
            popup.setOnMenuItemClickListener(item -> {
                lessonMenuSelected(item, schedule, lesson, weekday);
                return false;
            });
            popup.show();
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void lessonMenuSelected(MenuItem item, SLessons schedule, SLesson lesson, int weekday) {
        thread.run(() -> {
            log.v(TAG, "Lesson menu | popup item | clicked | " + item.getTitle().toString());
            switch (item.getItemId()) {
                case R.id.open_group: {
                    tabHostPresenter.setQuery(lesson.getGroup());
                    tabHostPresenter.invalidate(false);
                    break;
                }
                case R.id.open_teacher: {
                    tabHostPresenter.setQuery(lesson.getTeacherId());
                    tabHostPresenter.invalidate(false);
                    break;
                }
                case R.id.open_room: {
                    tabHostPresenter.setQuery(lesson.getRoom());
                    tabHostPresenter.invalidate(false);
                    break;
                }
                case R.id.open_location: {
                    thread.run(() -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + lesson.getBuilding()));
                        eventBus.fire(new OpenIntentEvent(intent).withIdentity(ScheduleLessonsTabFragmentPresenterImpl.class.getName()));
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
                case R.id.reduce_lesson: {
                    thread.run(() -> {
                        if (!scheduleLessonsHelper.reduceLesson(schedule.getQuery(), weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
                case R.id.restore_lesson: {
                    thread.run(() -> {
                        if (!scheduleLessonsHelper.restoreLesson(schedule.getQuery(), weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
                case R.id.delete_lesson: {
                    thread.run(() -> {
                        if (!scheduleLessonsHelper.deleteLesson(schedule.getQuery(), weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
                case R.id.copy_lesson: {
                    thread.run(() -> {
                        if (!scheduleLessonsHelper.createLesson(activity, schedule.getQuery(), schedule.getTitle(), schedule.getType(), weekday, lesson, null)) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
                case R.id.edit_lesson: {
                    thread.run(() -> {
                        if (!scheduleLessonsHelper.editLesson(activity, schedule.getQuery(), schedule.getTitle(), schedule.getType(), weekday, lesson, null)) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    break;
                }
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void bindMenuItem(Menu menu, int id, String text) {
        if (StringUtils.isNotBlank(text)) {
            menu.findItem(id).setTitle(text);
        } else {
            menu.findItem(id).setVisible(false);
        }
    }

    // -<-- Lesson menu --<- || -->- Teacher selection ->--
    
    private void teacherSelected(View view, RVALessons entity) {
        thread.run(() -> {
            if (entity.getTeacher() == null) {
                return;
            }
            if (StringUtils.isBlank(entity.getTeacher().getPersonId())) {
                return;
            }
            tabHostPresenter.setQuery(entity.getTeacher().getPersonId());
            tabHostPresenter.invalidate(false);
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    // -<-- Teacher selection --<- || -->- Utils ->--

    private SLesson makeLesson(String subject, String type, int parity, String timeStart, String timeEnd) {
        SLesson lesson = new SLesson();
        lesson.setSubject(subject);
        lesson.setType(type);
        lesson.setParity(parity);
        lesson.setTimeStart(timeStart);
        lesson.setTimeEnd(timeEnd);
        lesson.setCdoitmoType("synthetic");
        return lesson;
    }

    private String getDayTitle(SDay day) {
        switch (day.getWeekday()) {
            case 0: return activity.getString(R.string.monday);
            case 1: return activity.getString(R.string.tuesday);
            case 2: return activity.getString(R.string.wednesday);
            case 3: return activity.getString(R.string.thursday);
            case 4: return activity.getString(R.string.friday);
            case 5: return activity.getString(R.string.saturday);
            case 6: return activity.getString(R.string.sunday);
            default:
                /*TODO implement when isu will be ready
                расписание из ису, когда есть расписания на определенный день
                if ("date".equals(day.getType()) && StringUtils.isNotBlank(day.getTitle())) {
                    return day.getTitle();
                }
                */
                return activity.getString(R.string.unknown_day);
        }
    }

    private String getLessonDesc(SLesson lesson, String type) {
        switch (type) {
            case "group": return lesson.getTeacherName();
            case "teacher": return lesson.getGroup();
            case "mine":
            case "room": {
                if (StringUtils.isBlank(lesson.getGroup())) {
                    return lesson.getTeacherName();
                }
                String desc = lesson.getGroup();
                if (StringUtils.isNotBlank(lesson.getTeacherName())) {
                    desc += " (" + lesson.getTeacherName() + ")";
                }
                return desc;
            }
        }
        return null;
    }

    private String getLessonMeta(SLesson lesson, String type) {
        switch (type) {
            case "mine":
            case "group":
            case "teacher": {
                if (StringUtils.isBlank(lesson.getRoom())) {
                    return lesson.getBuilding();
                }
                String meta = activity.getString(R.string.room_short) + " " + lesson.getRoom();
                if (StringUtils.isNotBlank(lesson.getBuilding())) {
                    meta += " (" + lesson.getBuilding() + ")";
                }
                return meta;
            }
            case "room": {
                return lesson.getBuilding();
            }
        }
        return null;
    }

    // -<-- Utils --<-
}
