package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
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

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleExamsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.model.rva.RVAExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExam;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;

import javax.inject.Inject;

import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ScheduleExamsTabFragmentPresenterImpl implements ScheduleExamsTabFragmentPresenter {

    private static final String TAG = "SETabFragment";
    private final Schedule.Handler<SExams> scheduleHandler;
    private ConnectedActivity activity = null;
    private SExams schedule = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private View container = null;
    private boolean invalidate = false;
    private boolean invalidateRefresh = false;
    private int type = ScheduleExamsTabHostFragmentPresenter.DEFAULT_INVALID_TYPE;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    ScheduleExamsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;

    public ScheduleExamsTabFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        scheduleHandler = new Schedule.Handler<SExams>() {
            @Override
            public void onSuccess(final SExams schedule, final boolean fromCache) {
                thread.run(() -> {
                    ScheduleExamsTabFragmentPresenterImpl.this.schedule = schedule;
                    if ("teachers".equals(schedule.getType()) && schedule.getTeachers() != null &&
                        CollectionUtils.isNotEmpty(schedule.getTeachers().getTeachers()) &&
                        schedule.getTeachers().getTeachers().size() == 1
                    ) {
                        tabHostPresenter.setQuery(schedule.getTeachers().getTeachers().get(0).getPersonId());
                        tabHostPresenter.invalidate(false);
                        return;
                    }
                    // fetch only right exams
                    ArrayList<SSubject> subjects = new ArrayList<>();
                    for (SSubject subject : CollectionUtils.emptyIfNull(schedule.getSchedule())) {
                        if (ScheduleExamsTabFragmentPresenterImpl.this.type == 0 && "exam".equals(StringUtils.defaultIfBlank(subject.getType(), "exam")) ||
                            ScheduleExamsTabFragmentPresenterImpl.this.type == 1 && "credit".equals(StringUtils.defaultIfBlank(subject.getType(), "exam"))
                        ) {
                            subjects.add(subject);
                        }
                    }
                    // get rva adapter
                    ScheduleExamsRVA adapter = new ScheduleExamsRVA(schedule, subjects, type);
                    adapter.setClickListener(R.id.schedule_lessons_menu, ScheduleExamsTabFragmentPresenterImpl.this::examsMenuMore);
                    adapter.setClickListener(R.id.schedule_lessons_share, ScheduleExamsTabFragmentPresenterImpl.this::examsMenuShare);
                    adapter.setClickListener(R.id.exam_touch_icon, ScheduleExamsTabFragmentPresenterImpl.this::subjectMenu);
                    adapter.setClickListener(R.id.teacher_picker_item, ScheduleExamsTabFragmentPresenterImpl.this::teacherSelected);
                    thread.runOnUI(() -> {
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
                                int position = layoutManager.findFirstVisibleItemPosition();
                                View v = recyclerView.getChildAt(0);
                                int offset = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());
                                ScheduleExamsTabHostFragmentPresenter.Scroll scroll = tabHostPresenter.scroll().get(type, null);
                                if (scroll == null) {
                                    scroll = new ScheduleExamsTabHostFragmentPresenter.Scroll();
                                }
                                scroll.position = position;
                                scroll.offset = offset;
                                tabHostPresenter.scroll().put(type, scroll);
                            });
                        }
                        // scroll to previous position
                        ScheduleExamsTabHostFragmentPresenter.Scroll scroll = tabHostPresenter.scroll().get(type, null);
                        if (scroll != null) {
                            layoutManager.scrollToPositionWithOffset(scroll.position, scroll.offset);
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
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                thread.runOnUI(() -> {
                    ScheduleExamsTabFragmentPresenterImpl.this.schedule = null;
                    log.v(TAG, "onFailure | statusCode=", statusCode, " | state=", state);
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case Schedule.FAILED_OFFLINE: {
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_offline_text);
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
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
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
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_empty_query);
                            if (view != null) {
                                view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleExamsSearchActivity.class)));
                                view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleExamsFragment.class, null));
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_NOT_FOUND: {
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_nothing_to_display_compact);
                            if (view != null) {
                                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_INVALID_QUERY: {
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_text);
                            if (view != null) {
                                ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                draw(view);
                            }
                            break;
                        }
                        case Schedule.FAILED_PERSONAL_NEED_ISU: {
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_isu_required);
                            if (view != null) {
                                view.findViewById(R.id.open_isu_auth).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                                view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleExamsSearchActivity.class)));
                                view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleExamsFragment.class, null));
                                draw(view);
                            }
                            break;
                        }
                    }
                }, throwable -> {
                    log.exception(throwable);
                });
            }
            @Override
            public void onProgress(final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "onProgress | state=", state);
                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading_text);
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

    @Override
    public void onCreate(Bundle savedInstanceState, ConnectedActivity activity, Fragment fragment) {
        thread.run(() -> {
            this.activity = activity;
            Bundle bundle = fragment.getArguments();
            if (bundle != null && bundle.containsKey("type")) {
                type = bundle.getInt("type");
            } else {
                log.w(TAG, "onCreate | UNDEFINED TYPE, going to use DEFAULT_TYPE");
                type = ScheduleExamsTabHostFragmentPresenter.DEFAULT_TYPE;
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
                if (tabHostPresenter.scroll() != null && !tabHostPresenter.isSameQueryRequested()) {
                    tabHostPresenter.scroll().clear();
                }
                if (refresh) {
                    scheduleExams.search(tabHostPresenter.getQuery(), 0, scheduleHandler);
                } else {
                    scheduleExams.search(tabHostPresenter.getQuery(), scheduleHandler);
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
            View state_try_again = inflate(context, R.layout.state_failed_button);
            if (state_try_again != null) {
                state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load(false));
                draw(state_try_again);
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

    // -->- Exams global menu ->--

    private void examsMenuShare(View view, RVAExams entity) {
        thread.runOnUI(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            SparseArray<SSubject> menuSubjectsMap = new SparseArray<>();
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.schedule_exams_common_share);
            if (CollectionUtils.isEmpty(entity.getEvents())) {
                popup.getMenu().findItem(R.id.share_exam_schedule).setVisible(false);
            } else {
                Menu subMenu = popup.getMenu().findItem(R.id.share_exam_schedule).getSubMenu();
                for (SSubject subject : entity.getEvents()) {
                    int id = View.generateViewId();
                    subMenu.add(Menu.NONE, id, Menu.NONE, subject.getSubject());
                    menuSubjectsMap.put(id, subject);
                }
            }
            popup.setOnMenuItemClickListener(menuItem -> {
                thread.run(() -> {
                    switch (menuItem.getItemId()) {
                        case R.id.share_all_schedule: shareSchedule(entity.getEvents()); break;
                        default:
                            SSubject subject = menuSubjectsMap.get(menuItem.getItemId());
                            if (subject != null) {
                                shareSchedule(subject);
                            }
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
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void examsMenuMore(View view, RVAExams entity) {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            boolean isCached = cacheToken != null && StringUtils.isNotBlank(storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_exams#lessons#" + cacheToken, ""));
            thread.runOnUI(() -> {
                PopupMenu popup = new PopupMenu(activity, view);
                popup.inflate(R.menu.schedule_exams_common_more);
                popup.getMenu().findItem(R.id.toggle_cache).setChecked(isCached);
                popup.setOnMenuItemClickListener(menuItem -> {
                    thread.run(() -> {
                        switch (menuItem.getItemId()) {
                            case R.id.toggle_cache:
                                if (toggleCache()) {
                                    thread.runOnUI(() -> menuItem.setChecked(!isCached));
                                }
                                break;
                            case R.id.open_settings: activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleExamsFragment.class, null); break;
                        }
                    }, throwable -> {
                        log.exception(throwable);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    });
                    popup.dismiss();
                    return true;
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
        if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_exams#lessons#" + cacheToken)) {
            if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_exams#lessons#" + cacheToken)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                return true;
            } else {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return false;
            }
        }
        if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_exams#lessons#" + cacheToken, schedule.toJsonString())) {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
            return true;
        } else {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            return false;
        }
    }

    private void shareSchedule(ArrayList<SSubject> events) {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleExams.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        if (CollectionUtils.isEmpty(events)) {
            sb.append(activity.getString(R.string.no_exams));
        } else {
            int examsThisTerm = 0;
            for (SSubject subject : events) {
                if (subject == null) {
                    continue;
                }
                examsThisTerm++;
                sb.append("\n");
                shareScheduleAppendSubject(sb, subject);
            }
            if (examsThisTerm == 0) {
                sb.append(activity.getString(R.string.no_exams));
            }
        }
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_sexams_all"));
    }

    private void shareSchedule(SSubject subject) {
        thread.assertNotUI();
        if (schedule == null || subject == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleExams.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        shareScheduleAppendSubject(sb, subject);
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_sexams_subject"));
    }

    private void shareScheduleAppendSubject(StringBuilder sb, SSubject subject) {
        String desc = null;
        switch (schedule.getType()) {
            case "group": desc = subject.getTeacherName(); break;
            case "teacher": desc = subject.getGroup(); break;
        }
        sb.append(subject.getSubject());
        if (StringUtils.isNotBlank(desc)) {
            sb.append(" (").append(desc).append(")");
        }
        sb.append("\n");
        if (subject.getAdvice() != null && StringUtils.isNotBlank(subject.getAdvice().getDate())) {
            shareScheduleAppendEvent(sb, subject.getAdvice(), activity.getString(R.string.consult));
            sb.append("\n");
        }
        if (subject.getExam() != null && StringUtils.isNotBlank(subject.getExam().getDate())) {
            shareScheduleAppendEvent(sb, subject.getExam(), activity.getString("credit".equals(subject.getType()) ? R.string.credit : R.string.exam));
            sb.append("\n");
        }
    }

    private void shareScheduleAppendEvent(StringBuilder sb, SExam event, String title) {
        String date = event.getDate();
        String time = event.getTime();
        String room = event.getRoom();
        String building = event.getBuilding();
        if (StringUtils.isNotBlank(event.getBuilding())) {
            room = ((event.getRoom() == null ? "" : event.getRoom()) + " " + event.getBuilding()).trim();
        }
        if (StringUtils.isNotBlank(time)) {
            date = cuteDate(date + " " + time, " HH:mm");
        } else {
            date = cuteDate(date, "");
        }
        sb.append(title);
        sb.append(": ");
        sb.append(date);
        if (StringUtils.isNotBlank(room) || StringUtils.isNotBlank(building)) {
            sb.append(",");
        }
        if (StringUtils.isNotBlank(room)) {
            sb.append(" ");
            sb.append(room);
        }
        if (StringUtils.isNotBlank(building)) {
            sb.append(" ");
            sb.append(building);
        }
    }

    // -<-- Exams global menu --<- || -->- Subject menu ->--

    private void subjectMenu(View view, RVAExams entity) {
        thread.runOnUI(() -> {
            if (schedule == null || entity.getSubject() == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                return;
            }
            SSubject subject = entity.getSubject();
            PopupMenu popup = new PopupMenu(activity, view);
            Menu menu = popup.getMenu();
            popup.getMenuInflater().inflate(R.menu.schedule_exams_item, menu);
            switch (schedule.getType()) {
                case "group": {
                    menu.findItem(R.id.open_group).setVisible(false);
                    menu.findItem(R.id.open_teacher).setTitle(subject.getTeacherName());
                    menu.findItem(R.id.open_teacher).setVisible(true);
                    break;
                }
                case "teacher": {
                    menu.findItem(R.id.open_group).setTitle(activity.getString(R.string.group) + " " + subject.getGroup());
                    menu.findItem(R.id.open_group).setVisible(true);
                    menu.findItem(R.id.open_teacher).setVisible(false);
                    break;
                }
                default: {
                    menu.findItem(R.id.open_group).setVisible(false);
                    menu.findItem(R.id.open_teacher).setVisible(false);
                    break;
                }
            }
            popup.setOnMenuItemClickListener(item -> {
                thread.runOnUI(() -> {
                    subjectMenuSelected(item, schedule, subject);
                    popup.dismiss();
                });
                return true;
            });
            popup.show();
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private void subjectMenuSelected(MenuItem item, SExams schedule, SSubject subject) {
        thread.run(() -> {
            log.v(TAG, "Subject menu | popup item | clicked | " + item.getTitle().toString());
            switch (item.getItemId()) {
                case R.id.open_group: {
                    if (StringUtils.isNotBlank(subject.getGroup())) {
                        tabHostPresenter.setQuery(subject.getGroup());
                        tabHostPresenter.invalidate(false);
                    }
                    break;
                }
                case R.id.open_teacher: {
                    String query = subject.getTeacherId();
                    if (StringUtils.isBlank(query)) {
                        query = subject.getTeacherName();
                    }
                    if (StringUtils.isNotBlank(query)) {
                        tabHostPresenter.setQuery(query);
                        tabHostPresenter.invalidate(false);
                    }
                    break;
                }
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    // -<-- Subject menu --<- || -->- Teacher selection ->--

    private void teacherSelected(View view, RVAExams entity) {
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

    private String cuteDate(String date, String date_format_append) {
        try {
            String date_format = "dd.MM.yyyy" + date_format_append;
            if (isValidFormat(date, date_format)) {
                date = textUtils.cuteDate(activity, storagePref, date_format, date);
            } else {
                Matcher m = ScheduleExamsRVA.patternBrokenDate.matcher(date);
                if (m.find()) {
                    String d = m.group(2);
                    String dt = d.trim();
                    if (dt.startsWith("янв")) d = ".01";
                    if (dt.startsWith("фев")) d = ".02";
                    if (dt.startsWith("мар")) d = ".03";
                    if (dt.startsWith("апр")) d = ".04";
                    if (dt.startsWith("май")) d = ".05";
                    if (dt.startsWith("июн")) d = ".06";
                    if (dt.startsWith("июл")) d = ".07";
                    if (dt.startsWith("авг")) d = ".08";
                    if (dt.startsWith("сен")) d = ".09";
                    if (dt.startsWith("окт")) d = ".10";
                    if (dt.startsWith("ноя")) d = ".11";
                    if (dt.startsWith("дек")) d = ".12";
                    date = m.group(1) + d + m.group(3);
                }
                date_format = "dd.MM" + date_format_append;
                if (isValidFormat(date, date_format)) {
                    date = cuteDateWOYear(date_format, date);
                }
            }
        } catch (Exception ignore) {/* ignore */}
        return date;
    }

    private String cuteDateWOYear(String date_format, String date_string) throws Exception {
        SimpleDateFormat format_input = new SimpleDateFormat(date_format, textUtils.getLocale(activity, storagePref));
        Calendar date = time.getCalendar();
        date.setTime(format_input.parse(date_string));
        return (new StringBuilder())
                .append(date.get(Calendar.DATE))
                .append(" ")
                .append(time.getGenitiveMonth(activity, date.get(Calendar.MONTH)))
                .append(" ")
                .append(textUtils.ldgZero(date.get(Calendar.HOUR_OF_DAY)))
                .append(":")
                .append(textUtils.ldgZero(date.get(Calendar.MINUTE)))
                .toString();
    }

    private boolean isValidFormat(String value, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, textUtils.getLocale(activity, storagePref));
            Date date = sdf.parse(value);
            if (!value.equals(sdf.format(date))) {
                date = null;
            }
            return date != null;
        } catch (Exception e) {
            return false;
        }
    }

    // -<-- Utils --<-
}
