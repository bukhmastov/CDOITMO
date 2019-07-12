package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsModifyFragment;
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
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import dagger.Lazy;

import static com.bukhmastov.cdoitmo.util.Thread.SL;

public class ScheduleLessonsTabFragmentPresenterImpl implements Schedule.Handler<SLessons>, ScheduleLessonsTabFragmentPresenter {

    private static final String TAG = "SLTabFragment";
    private ConnectedActivity activity = null;
    private SLessons schedule = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private View container = null;
    private boolean invalidate = false;
    private boolean invalidateRefresh = false;
    /**
     * 0 - Нечетная
     * 1 - Четная
     * 2 - Обе недели
     */
    private int type = ScheduleLessonsTabHostFragmentPresenter.DEFAULT_INVALID_TYPE;
    private List<String> selfGroups = new ArrayList<>();

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
    @Inject
    Account account;
    @Inject
    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;

    public ScheduleLessonsTabFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
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
        thread.initialize(SL);
        thread.run(SL, () -> {
            this.activity = activity;
            Bundle bundle = fragment.getArguments();
            if (bundle != null && bundle.containsKey("type")) {
                type = bundle.getInt("type");
            } else {
                log.w(TAG, "onCreate | UNDEFINED TYPE, going to use DEFAULT_TYPE");
                type = ScheduleLessonsTabHostFragmentPresenter.DEFAULT_TYPE;
            }
            log.v(TAG, "Fragment created | type=", type);
            selfGroups.clear();
            selfGroups.addAll(account.getGroups(activity));
            tabHostPresenter.tabs().put(type, refresh -> thread.run(SL, () -> {
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
        thread.run(SL, () -> {
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
        thread.run(SL, () -> {
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
        log.v(TAG, "Fragment paused | type=", type);
        if (requestHandle != null) {
            thread.standalone(() -> {
                if (requestHandle != null && requestHandle.cancel()) {
                    log.v(TAG, "Fragment paused | type=", type, " | paused and requested reload");
                    loaded = false;
                }
            });
        }
    }

    private void load(boolean refresh) {
        thread.runOnUI(SL, () -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                failed(activity);
                return;
            }
            draw(activity, R.layout.state_loading_text);
            if (activity == null || tabHostPresenter.getQuery() == null) {
                log.w(TAG, "load | some values are null | activity=", activity, " | getQuery()=", tabHostPresenter.getQuery());
                failed(activity);
                return;
            }
            if (!tabHostPresenter.isSameQueryRequested()) {
                tabHostPresenter.scroll().clear();
            }
            // Standalone thread because this fragment going to be loaded up to three times
            thread.standalone(() -> {
                if (refresh) {
                    scheduleLessons.search(tabHostPresenter.getQuery(), 0, this);
                } else {
                    scheduleLessons.search(tabHostPresenter.getQuery(), this);
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

    @Override
    public void onSuccess(SLessons schedule, boolean fromCache) {
        thread.run(SL, () -> {
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
            adapter.setClickListener(R.id.schedule_lessons_menu, ScheduleLessonsTabFragmentPresenterImpl.this::lessonsMenuMore);
            adapter.setClickListener(R.id.schedule_lessons_share, ScheduleLessonsTabFragmentPresenterImpl.this::lessonsMenuShare);
            adapter.setClickListener(R.id.schedule_lessons_create, ScheduleLessonsTabFragmentPresenterImpl.this::lessonsMenuCreate);
            adapter.setClickListener(R.id.lesson_touch_icon, ScheduleLessonsTabFragmentPresenterImpl.this::lessonMenu);
            adapter.setClickListener(R.id.teacher_picker_item, ScheduleLessonsTabFragmentPresenterImpl.this::teacherSelected);
            adapter.setClickListener(R.id.day_info, ScheduleLessonsTabFragmentPresenterImpl.this::dayInfoClicked);
            thread.runOnUI(SL, () -> {
                draw(activity, R.layout.layout_schedule_both_recycle_list);
                // prepare
                SwipeRefreshLayout swipe = container.findViewById(R.id.schedule_swipe);
                RecyclerView recyclerView = container.findViewById(R.id.schedule_list);
                if (swipe == null || recyclerView == null) {
                    return;
                }
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
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
                    Calendar today = time.getCalendar();
                    int weekdayToday = today.get(Calendar.DAY_OF_WEEK);
                    switch (weekdayToday) {
                        case Calendar.MONDAY: {
                            position = getRvaNearestScrollPosition(adapter, 0, today, Calendar.MONDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.TUESDAY: {
                            position = getRvaNearestScrollPosition(adapter, 1, today, Calendar.TUESDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.WEDNESDAY: {
                            position = getRvaNearestScrollPosition(adapter, 2, today, Calendar.WEDNESDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.THURSDAY: {
                            position = getRvaNearestScrollPosition(adapter, 3, today, Calendar.THURSDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.FRIDAY: {
                            position = getRvaNearestScrollPosition(adapter, 4, today, Calendar.FRIDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.SATURDAY: {
                            position = getRvaNearestScrollPosition(adapter, 5, today, Calendar.SATURDAY);
                            if (position >= 0) break;
                        }
                        case Calendar.SUNDAY: {
                            position = getRvaNearestScrollPosition(adapter, 6, today, Calendar.SUNDAY);
                            if (position >= 0) break;
                        }
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
    public void onFailure(int code, Client.Headers headers, int state) {
        thread.runOnUI(SL, () -> {
            ScheduleLessonsTabFragmentPresenterImpl.this.schedule = null;
            log.v(TAG, "onFailure | code=", code, " | state=", state);
            if (state == Client.FAILED_OFFLINE) {
                ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_offline_text);
                if (view != null) {
                    view.findViewById(R.id.offline_reload).setOnClickListener(v -> load(false));
                    draw(view);
                }
                return;
            }
            if (state == Schedule.FAILED_NOT_FOUND) {
                ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_nothing_to_display_compact);
                if (view != null) {
                    ((TextView) view.findViewById(R.id.ntd_text)).setText(scheduleLessons.getFailedMessage(code, state));
                    draw(view);
                }
                return;
            }
            if (state == Schedule.FAILED_PERSONAL_NEED_ISU) {
                ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_isu_required);
                if (view != null) {
                    view.findViewById(R.id.open_isu_auth).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleLessonsSearchActivity.class)));
                    view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null));
                }
                draw(view);
                return;
            }
            if (state == Schedule.FAILED_EMPTY_QUERY) {
                ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_empty_query);
                if (view != null) {
                    view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleLessonsSearchActivity.class)));
                    view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null));
                    draw(view);
                }
                return;
            }
            if (state == Schedule.FAILED_INVALID_QUERY) {
                ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_text);
                if (view != null) {
                    ((TextView) view.findViewById(R.id.text)).setText(scheduleLessons.getFailedMessage(code, state));
                    draw(view);
                }
                return;
            }
            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
            if (view != null) {
                ((TextView) view.findViewById(R.id.try_again_message)).setText(scheduleLessons.getFailedMessage(code, state));
                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                draw(view);
            }
        }, throwable -> {
            log.exception(throwable);
            failed(activity);
        });
    }

    @Override
    public void onProgress(int state) {
        thread.runOnUI(SL, () -> {
            log.v(TAG, "onProgress | state=", state);
            ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading_text);
            if (view != null) {
                String message = scheduleLessons.getProgressMessage(state);
                ((TextView) view.findViewById(R.id.loading_message)).setText(message);
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

    private int getRvaNearestScrollPosition(ScheduleLessonsRVA adapter, int weekday, Calendar today, int weekdayCompare) {
        int position = adapter.getWeekdayPosition(weekday);
        if (position < 0 && today.get(Calendar.DAY_OF_WEEK) == weekdayCompare) {
            position = adapter.getCustomDayPosition(time.getScheduleCustomDayRaw(today));
        }
        return position;
    }

    private void draw(View view) {
        thread.runOnUI(SL, () -> {
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

    private void lessonsMenuCreate(View view, RVALessons entity) {
        thread.runOnUI(SL, () -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.schedule_lessons_common_create);
            popup.setOnMenuItemClickListener(menuItem -> {
                thread.run(SL, () -> {
                    switch (menuItem.getItemId()) {
                        case R.id.add_lesson: addLesson(entity.getParity()); logStatistic("schedule_create_lesson"); break;
                        case R.id.add_military_day_monday: addMilitaryDay(0); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_tuesday: addMilitaryDay(1); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_wednesday: addMilitaryDay(2); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_thursday: addMilitaryDay(3); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_friday: addMilitaryDay(4); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_saturday: addMilitaryDay(5); logStatistic("schedule_create_military"); break;
                        case R.id.add_military_day_sunday: addMilitaryDay(6); logStatistic("schedule_create_military"); break;
                    }
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
                popup.dismiss();
                return true;
            });
            popup.show();
            logStatistic("schedule_create");
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void lessonsMenuShare(View view, RVALessons entity) {
        thread.runOnUI(SL, () -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            SparseArray<String> customDaysMap = new SparseArray<>();
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.schedule_lessons_common_share);
            if (CollectionUtils.isEmpty(entity.getDays())) {
                popup.getMenu().findItem(R.id.share_daily_schedule).setVisible(false);
            } else {
                Menu subMenu = popup.getMenu().findItem(R.id.share_daily_schedule).getSubMenu();
                for (SDay day : entity.getDays()) {
                    int id = getDayResID(day.getWeekday());
                    Integer weekday = day.getWeekday();
                    String title = getDayTitle(day);
                    if (weekday == null) {
                        customDaysMap.append(id, day.getTitle());
                    }
                    subMenu.add(Menu.NONE, id, weekday != null ? weekday : Menu.NONE, title);
                }
            }
            popup.setOnMenuItemClickListener(menuItem -> {
                thread.run(SL, () -> {
                    switch (menuItem.getItemId()) {
                        case R.id.share_all_schedule: shareSchedule(entity.getDays()); logStatistic("schedule_share_all"); break;
                        case R.id.share_daily_schedule: logStatistic("schedule_share_day"); break;
                        case R.id.share_changes: shareChanges(); logStatistic("schedule_share_changes"); break;
                        case R.id.monday: shareSchedule(entity.getDays(), 0, null); break;
                        case R.id.tuesday: shareSchedule(entity.getDays(), 1, null); break;
                        case R.id.wednesday: shareSchedule(entity.getDays(), 2, null); break;
                        case R.id.thursday: shareSchedule(entity.getDays(), 3, null); break;
                        case R.id.friday: shareSchedule(entity.getDays(), 4, null); break;
                        case R.id.saturday: shareSchedule(entity.getDays(), 5, null); break;
                        case R.id.sunday: shareSchedule(entity.getDays(), 6, null); break;
                        default:
                            String customDay = customDaysMap.get(menuItem.getItemId());
                            shareSchedule(entity.getDays(), null, customDay);
                            break;
                    }
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
                popup.dismiss();
                return true;
            });
            popup.show();
            logStatistic("schedule_share");
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void lessonsMenuMore(View view, RVALessons entity) {
        thread.run(SL, () -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            boolean isCached = cacheToken != null && StringUtils.isNotBlank(storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, ""));
            thread.runOnUI(SL, () -> {
                PopupMenu popup = new PopupMenu(activity, view);
                popup.inflate(R.menu.schedule_lessons_common_more);
                popup.getMenu().findItem(R.id.toggle_cache).setChecked(isCached);
                popup.setOnMenuItemClickListener(menuItem -> {
                    thread.run(SL, () -> {
                        switch (menuItem.getItemId()) {
                            case R.id.toggle_cache:
                                if (toggleCache()) {
                                    thread.runOnUI(SL, () -> menuItem.setChecked(!isCached));
                                    logStatistic("schedule_more_cache_" + (isCached ? "removed" : "added"));
                                }
                                break;
                            case R.id.create_widget: createWidget(); logStatistic("schedule_more_create_widget"); break;
                            case R.id.remove_changes: clearChanges(); logStatistic("schedule_more_clear_changes"); break;
                            case R.id.open_settings:
                                thread.runOnUI(SL, () -> {
                                    activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null);
                                    logStatistic("schedule_more_open_settings");
                                });
                                break;
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    popup.dismiss();
                    return true;
                });
                if (!isPinWidgetSupported()) {
                    popup.getMenu().findItem(R.id.create_widget).setVisible(false);
                }
                popup.show();
                logStatistic("schedule_more");
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private boolean toggleCache() throws Throwable {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            return false;
        }
        String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
        if (cacheToken == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            return false;
        }
        if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
            if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                return true;
            } else {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return false;
            }
        }
        if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, schedule.toJsonString())) {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
            return true;
        } else {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            return false;
        }
    }

    private void addLesson(int parity) {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        SLesson lesson = new SLesson();
        lesson.setParity(parity);
        if (!scheduleLessonsHelper.createLesson(schedule.getQuery(), schedule.getTitle(), schedule.getType(), time.getWeekDay(), null, lesson, extras -> {
            thread.runOnUI(SL, () -> {
                activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
            });
        })) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        }
    }

    private void addMilitaryDay(int dayOfWeek) {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("Утренний осмотр, строевая подготовка", "Военка", 2, "9:05", "9:20"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("1 пара", "Военка", 2, "9:30", "10:50"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("2 пара", "Военка", 2, "11:00", "12:20"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("3 пара", "Военка", 2, "12:30", "13:50"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("4 пара", "Военка", 2, "14:50", "16:10"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("Строевая подготовка", "Военка", 2, "16:20", "16:35"), null);
        scheduleLessonsHelper.createLesson(schedule.getQuery(), dayOfWeek, null, makeLesson("Кураторский час", "Военка", 2, "16:45", "17:30"), null);
        tabHostPresenter.invalidateOnDemand();
    }

    private void shareSchedule(ArrayList<SDay> days) {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleLessons.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        if (CollectionUtils.isEmpty(days)) {
            sb.append(activity.getString(R.string.no_lessons));
        } else {
            if (type == 2 && shareScheduleIsScheduleHasEvenOddWeekLessons(days)) {
                shareScheduleAppendLessonsForWeek(sb, days, schedule.getType(), 0);
                shareScheduleAppendLessonsForWeek(sb, days, schedule.getType(), 1);
            } else {
                shareScheduleAppendLessonsForWeek(sb, days, schedule.getType(), 2);
            }
        }
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_slessons_all"));
    }

    private void shareSchedule(ArrayList<SDay> days, Integer weekday, String customDay) {
        thread.assertNotUI();
        if (schedule == null || CollectionUtils.isEmpty(days)) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        if (weekday == null && customDay == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        SDay day = null;
        for (SDay d : days) {
            if (d.isMatched(weekday, customDay)) {
                day = d;
                break;
            }
        }
        if (day == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleLessons.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        sb.append(getDayTitle(day));
        sb.append("\n");
        if (CollectionUtils.isNotEmpty(day.getLessons())) {
            shareScheduleAppendLessons(sb, day.getLessons(), schedule.getScheduleType(), true);
        } else {
            sb.append(activity.getString(R.string.no_lessons));
        }
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_slessons_day"));
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

    private void shareScheduleAppendLessonsForWeek(StringBuilder sb, ArrayList<SDay> days, String scheduleType, int parity) {
        if (CollectionUtils.isEmpty(days)) {
            return;
        }
        StringBuilder sbb = new StringBuilder();
        if (parity == 0 || parity == 1) {
            sbb.append("\n");
            sbb.append(activity.getString(parity == 0 ? R.string.week_even : R.string.week_odd).toUpperCase());
            sbb.append("\n");
        }
        int lessonsThisWeek = 0;
        TreeSet<SDay> daysSorted = scheduleLessonsHelper.filterAndSortDays(days);
        for (SDay day : daysSorted) {
            if (day == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            TreeSet<SLesson> lessons = scheduleLessonsHelper.filterAndSortLessons(day.getLessons(), parity, true);
            if (CollectionUtils.isEmpty(lessons)) {
                continue;
            }
            sbb.append("\n");
            sbb.append(getDayTitle(day));
            sbb.append("\n");
            lessonsThisWeek += shareScheduleAppendLessons(sbb, lessons, scheduleType, parity == 2);
        }
        if (lessonsThisWeek > 0) {
            sb.append(sbb);
        }
    }

    private int shareScheduleAppendLessons(StringBuilder sb, Collection<SLesson> lessons, String scheduleType, boolean displayParity) {
        int lessonsCount = 0;
        for (SLesson lesson : lessons) {
            if (lesson == null) {
                continue;
            }
            lessonsCount++;
            String subject = lesson.getSubjectWithNote();
            String desc = getLessonDesc(lesson, scheduleType);
            String meta = getLessonMeta(lesson, scheduleType);
            String lessonType = lesson.getType();
            if (StringUtils.isNotBlank(lessonType)) {
                switch (lessonType) {
                    case "practice": lessonType = activity.getString(R.string.practice); break;
                    case "lecture": lessonType = activity.getString(R.string.lecture); break;
                    case "lab": lessonType = activity.getString(R.string.lab); break;
                    case "iws": lessonType = activity.getString(R.string.iws); break;
                }
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
            if (displayParity && lesson.getParity() != 2) {
                sb.append("(").append(lesson.getParity() == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd)).append(") ");
            }
            sb.append("\n");
        }
        return lessonsCount;
    }

    private void shareChanges() {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        Bundle extras = new Bundle();
        extras.putString("action", "share");
        extras.putString("query", schedule.getQuery());
        extras.putString("type", schedule.getType());
        extras.putString("title", schedule.getTitle());
        thread.runOnUI(SL, () -> activity.openActivityOrFragment(ScheduleLessonsShareFragment.class, extras));
    }

    private void clearChanges() {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                .setMessage(R.string.pref_schedule_lessons_clear_direct_additional_warning)
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(R.string.proceed, (dialog, which) -> thread.standalone(() -> {
                    if (!scheduleLessonsHelper.clearChanges(schedule.getQuery(), () -> tabHostPresenter.invalidateOnDemand())) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.no_changes));
                    }
                }))
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createWidget() {
        if (!isPinWidgetSupported()) {
            notificationMessage.snackBar(activity, activity.getString(R.string.pin_app_widget_not_supported));
            return;
        }
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        ComponentName componentName = new ComponentName(activity, ScheduleLessonsWidget.class);
        Intent intent = new Intent(activity, ScheduleLessonsWidget.class);
        intent.setAction(ScheduleLessonsWidget.ACTION_WIDGET_OPEN_CONFIGURATION);
        intent.putExtra(ScheduleLessonsWidget.EXTRA_WIDGET_WSL_SETTINGS_QUERY, schedule.getQuery());
        PendingIntent onSuccess = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (!appWidgetManager.requestPinAppWidget(componentName, null, onSuccess)) {
            notificationMessage.snackBar(activity, activity.getString(R.string.pin_app_widget_not_supported));
        }
    }

    // -<-- Lessons global menu --<- || -->- Lesson menu ->--

    private void lessonMenu(View view, RVALessons entity) {
        thread.runOnUI(SL, () -> {
            if (schedule == null || entity.getLesson() == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            SLesson lesson = entity.getLesson();
            Integer weekday = entity.getWeekday();
            String customDay = entity.getCustomDay();
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.schedule_lessons_item);
            bindMenuItem(popup, R.id.open_group, "group".equals(schedule.getType()) || StringUtils.isBlank(lesson.getGroup()) ? null : activity.getString(R.string.group) + " " + lesson.getGroup());
            bindMenuItem(popup, R.id.open_teacher, "teacher".equals(schedule.getType()) || StringUtils.isBlank(lesson.getTeacherName()) || StringUtils.isBlank(lesson.getTeacherId()) ? null : lesson.getTeacherName());
            bindMenuItem(popup, R.id.open_room, "room".equals(schedule.getType()) || StringUtils.isBlank(lesson.getRoom()) ? null : activity.getString(R.string.room) + " " + lesson.getRoom());
            bindMenuItem(popup, R.id.open_location, lesson.getBuilding());
            bindMenuItem(popup, R.id.reduce_lesson, "normal".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.reduce_lesson) : null);
            bindMenuItem(popup, R.id.restore_lesson, "reduced".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.restore_lesson) : null);
            bindMenuItem(popup, R.id.delete_lesson, "synthetic".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.delete_lesson) : null);
            bindMenuItem(popup, R.id.edit_lesson, "synthetic".equals(lesson.getCdoitmoType()) ? activity.getString(R.string.edit_lesson) : null);
            popup.setOnMenuItemClickListener(item -> {
                thread.runOnUI(SL, () -> {
                    lessonMenuSelected(item, schedule, lesson, weekday, customDay);
                    popup.dismiss();
                });
                return true;
            });
            popup.show();
            logStatistic("lesson");
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void lessonMenuSelected(MenuItem item, SLessons schedule, SLesson lesson, Integer weekday, String customDay) {
        thread.run(SL, () -> {
            log.v(TAG, "Lesson menu | popup item | clicked | " + item.getTitle().toString());
            switch (item.getItemId()) {
                case R.id.open_group: {
                    tabHostPresenter.setQuery(lesson.getGroup());
                    tabHostPresenter.invalidate(false);
                    logStatistic("lesson_open_group");
                    break;
                }
                case R.id.open_teacher: {
                    tabHostPresenter.setQuery(lesson.getTeacherId());
                    tabHostPresenter.invalidate(false);
                    logStatistic("lesson_open_teacher");
                    break;
                }
                case R.id.open_room: {
                    tabHostPresenter.setQuery(lesson.getRoom());
                    tabHostPresenter.invalidate(false);
                    logStatistic("lesson_open_room");
                    break;
                }
                case R.id.open_location: {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + lesson.getBuilding()));
                    eventBus.fire(new OpenIntentEvent(intent).withIdentity(ScheduleLessonsTabFragmentPresenterImpl.class.getName()));
                    logStatistic("lesson_open_location");
                    break;
                }
                case R.id.open_edit_actions: {
                    logStatistic("lesson_action");
                    break;
                }
                case R.id.reduce_lesson: {
                    if (!scheduleLessonsHelper.reduceLesson(schedule.getQuery(), weekday, customDay, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    logStatistic("lesson_action_reduce_lesson");
                    break;
                }
                case R.id.restore_lesson: {
                    if (!scheduleLessonsHelper.restoreLesson(schedule.getQuery(), weekday, customDay, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    logStatistic("lesson_action_restore_lesson");
                    break;
                }
                case R.id.delete_lesson: {
                    if (!scheduleLessonsHelper.deleteLesson(schedule.getQuery(), weekday, customDay, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    logStatistic("lesson_action_delete_lesson");
                    break;
                }
                case R.id.copy_lesson: {
                    if (!scheduleLessonsHelper.createLesson(schedule.getQuery(), schedule.getTitle(), schedule.getType(), weekday, customDay, lesson, extras -> {
                        thread.runOnUI(SL, () -> {
                            activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
                        });
                    })) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    logStatistic("lesson_action_copy_lesson");
                    break;
                }
                case R.id.edit_lesson: {
                    if (!scheduleLessonsHelper.editLesson(schedule.getQuery(), schedule.getTitle(), schedule.getType(), weekday, customDay, lesson, extras -> {
                        thread.runOnUI(SL, () -> {
                            activity.openActivityOrFragment(ScheduleLessonsModifyFragment.class, extras);
                        });
                    })) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                    logStatistic("lesson_action_edit_lesson");
                    break;
                }
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void bindMenuItem(PopupMenu popup, int id, String text) {
        if (StringUtils.isNotBlank(text)) {
            popup.getMenu().findItem(id).setTitle(text);
        } else {
            popup.getMenu().findItem(id).setVisible(false);
        }
    }

    // -<-- Lesson menu --<- || -->- Teacher selection ->--

    private void teacherSelected(View view, RVALessons entity) {
        thread.run(SL, () -> {
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

    private void dayInfoClicked(View view, RVALessons entity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.unknown_day)
                .setMessage(R.string.unknown_day_about)
                .setIcon(R.drawable.ic_help)
                .setNegativeButton(R.string.close, null)
                .create().show();
        logStatistic("schedule_unknown_day_clicked");
    }

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

    private @IdRes int getDayResID(Integer weekday) {
        if (weekday == null) {
            return View.generateViewId();
        }
        switch (weekday) {
            case 0: return R.id.monday;
            case 1: return R.id.tuesday;
            case 2: return R.id.wednesday;
            case 3: return R.id.thursday;
            case 4: return R.id.friday;
            case 5: return R.id.saturday;
            case 6: return R.id.sunday;
        }
        return View.generateViewId();
    }

    private String getDayTitle(SDay day) {
        if (day.getWeekday() == null) {
            if (StringUtils.isBlank(day.getTitle())) {
                return activity.getString(R.string.unknown_day);
            }
            return time.getScheduleCustomDayTitle(activity, day.getTitle());
        }
        switch (day.getWeekday()) {
            case 0: return activity.getString(R.string.monday);
            case 1: return activity.getString(R.string.tuesday);
            case 2: return activity.getString(R.string.wednesday);
            case 3: return activity.getString(R.string.thursday);
            case 4: return activity.getString(R.string.friday);
            case 5: return activity.getString(R.string.saturday);
            case 6: return activity.getString(R.string.sunday);
            default: return activity.getString(R.string.unknown_day);
        }
    }

    private String getLessonDesc(SLesson lesson, String type) {
        switch (type) {
            case "group": return lesson.getTeacherName();
            case "teacher": return lesson.getGroup();
            case "personal":
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
            case "personal":
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

    private boolean isPinWidgetSupported() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return false;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        return appWidgetManager.isRequestPinAppWidgetSupported();
    }

    private void logStatistic(String info) {
        String parity = "both";
        if (type == 0) parity = "odd";
        if (type == 1) parity = "even";
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_TYPE, info);
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_PARITY, parity);
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_QUERY, schedule.getQuery());
        bundle.putString(FirebaseAnalyticsProvider.Param.SCHEDULE_LESSONS_QUERY_IS_SELF, selfGroups.contains(schedule.getQuery()) ? "1" : "0");
        firebaseAnalyticsProvider.get().logEvent(
                activity,
                FirebaseAnalyticsProvider.Event.SCHEDULE_LESSONS,
                bundle
        );
    }
    
    // -<-- Utils --<-
}
