package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleAttestationsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleAttestationsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleAttestationsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.model.rva.RVAAttestations;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestation;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SSubject;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ScheduleAttestationsFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements ScheduleAttestationsFragmentPresenter {

    private static final String TAG = "SAFragment";

    private interface TabProvider {
        void onInvalidate(boolean refresh);
    }
    private class Scroll {
        public int position = 0;
        public int offset = 0;
    }

    private final Schedule.Handler<SAttestations> scheduleHandler;
    private SAttestations schedule = null;
    private String lastQuery = null;
    private String query = null;
    private Scroll scroll = null;
    private TabProvider tab = null;
    private boolean invalidate = false;
    private boolean invalidate_refresh = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleAttestations scheduleAttestations;
    @Inject
    Storage storage;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleAttestationsFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
        scheduleHandler = new Schedule.Handler<SAttestations>() {
            @Override
            public void onSuccess(final SAttestations schedule, final boolean fromCache) {
                thread.run(() -> {
                    ScheduleAttestationsFragmentPresenterImpl.this.schedule = schedule;
                    int week = time.getWeek(activity);
                    ScheduleAttestationsRVA adapter = new ScheduleAttestationsRVA(schedule, week);
                    adapter.setClickListener(R.id.schedule_lessons_menu, ScheduleAttestationsFragmentPresenterImpl.this::attestationsMenuMore);
                    adapter.setClickListener(R.id.schedule_lessons_share, ScheduleAttestationsFragmentPresenterImpl.this::attestationsMenuShare);
                    thread.runOnUI(() -> {
                        fragment.draw(R.layout.layout_schedule_both_recycle_list);
                        // prepare
                        SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.schedule_swipe);
                        RecyclerView recyclerView = fragment.container().findViewById(R.id.schedule_list);
                        if (swipe == null || recyclerView == null) {
                            return;
                        }
                        LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
                        // swipe
                        swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                        swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                        swipe.setOnRefreshListener(() -> {
                            swipe.setRefreshing(false);
                            load(true);
                        });
                        // recycle view (list)
                        recyclerView.setLayoutManager(layoutManager);
                        recyclerView.setAdapter(adapter);
                        recyclerView.setHasFixedSize(true);
                        // scroll to prev position listener (only android 23+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            recyclerView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                                final int position = layoutManager.findFirstVisibleItemPosition();
                                final View v = recyclerView.getChildAt(0);
                                final int offset = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());
                                if (scroll == null) {
                                    scroll = new Scroll();
                                }
                                scroll.position = position;
                                scroll.offset = offset;
                            });
                        }
                        // scroll to previous position
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
                    ScheduleAttestationsFragmentPresenterImpl.this.schedule = null;
                    log.v(TAG, "onFailure | statusCode=", statusCode, " | state=", state);
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case Schedule.FAILED_OFFLINE: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_offline_text);
                            view.findViewById(R.id.offline_reload).setOnClickListener(v -> load(false));
                            fragment.draw(view);
                            break;
                        }
                        case Client.FAILED_TRY_AGAIN:
                        case Client.FAILED_SERVER_ERROR:
                        case Schedule.FAILED_LOAD: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_failed_button);
                            if (state == Client.FAILED_TRY_AGAIN) {
                                ((TextView) view.findViewById(R.id.try_again_message)).setText(Client.getFailureMessage(activity, statusCode));
                            }
                            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                            fragment.draw(view);
                            break;
                        }
                        case Schedule.FAILED_EMPTY_QUERY: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.layout_schedule_empty_query);
                            view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleAttestationsSearchActivity.class)));
                            view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null));
                            fragment.draw(view);
                            break;
                        }
                        case Schedule.FAILED_NOT_FOUND: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_nothing_to_display_compact);
                            ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                            fragment.draw(view);
                            break;
                        }
                        case Schedule.FAILED_INVALID_QUERY: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_failed_text);
                            ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                            fragment.draw(view);
                            break;
                        }
                        case Schedule.FAILED_PERSONAL_NEED_ISU: {
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.layout_schedule_isu_required);
                            view.findViewById(R.id.open_isu_auth).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                            view.findViewById(R.id.open_search).setOnClickListener(v -> eventBus.fire(new OpenActivityEvent(ScheduleAttestationsSearchActivity.class)));
                            view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null));
                            fragment.draw(view);
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
                    final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_loading_text);
                    ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                    fragment.draw(view);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            // define query
            String scope = fragment.restoreData();
            if (scope == null) {
                scope = scheduleAttestations.getDefaultScope();
            }
            final Intent intent = activity.getIntent();
            if (intent != null && intent.hasExtra("action_extra")) {
                String action_extra = intent.getStringExtra("action_extra");
                if (action_extra != null && !action_extra.isEmpty()) {
                    intent.removeExtra("action_extra");
                    scope = action_extra;
                }
            }
            setQuery(scope);
            fragment.storeData(scope);
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            tab = null;
            scroll = null;
            if (fragment != null && fragment.toolbar() != null) {
                MenuItem action_search = fragment.toolbar().findItem(R.id.action_search);
                if (action_search != null && action_search.isVisible()) {
                    log.v(TAG, "Hiding action_search");
                    action_search.setVisible(false);
                    action_search.setOnMenuItemClickListener(null);
                }
            }
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            thread.runOnUI(() -> {
                if (fragment != null && fragment.toolbar() != null) {
                    MenuItem action_search = fragment.toolbar().findItem(R.id.action_search);
                    if (action_search != null && !action_search.isVisible()) {
                        log.v(TAG, "Revealing action_search");
                        action_search.setVisible(true);
                        action_search.setOnMenuItemClickListener(item -> {
                            thread.run(() -> {
                                log.v(TAG, "action_search clicked");
                                eventBus.fire(new OpenActivityEvent(ScheduleAttestationsSearchActivity.class));
                            });
                            return false;
                        });
                    }
                }
            });
            if (tab == null) {
                tab = refresh -> thread.run(() -> {
                    log.v(TAG, "onInvalidate | refresh=", refresh);
                    fragment.storeData(getQuery());
                    if (fragment.isResumed()) {
                        invalidate = false;
                        invalidate_refresh = false;
                        load(refresh);
                    } else {
                        invalidate = true;
                        invalidate_refresh = refresh;
                    }
                });
            }
            if (invalidate) {
                invalidate = false;
                loaded = true;
                load(invalidate_refresh);
                invalidate_refresh = false;
            } else if (!loaded) {
                loaded = true;
                load(false);
            }
        });
    }

    private void load(boolean refresh) {
        thread.runOnUI(() -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                failed(activity);
                return;
            }
            fragment.draw(R.layout.state_loading_text);
            thread.run(() -> {
                if (activity == null || getQuery() == null) {
                    log.w(TAG, "load | some values are null | activity=", activity, " | getQuery()=", getQuery());
                    failed(activity);
                    return;
                }
                if (scroll != null && !isSameQueryRequested()) {
                    scroll.position = 0;
                    scroll.offset = 0;
                }
                if (refresh) {
                    scheduleAttestations.search(getQuery(), 0, scheduleHandler);
                } else {
                    scheduleAttestations.search(getQuery(), scheduleHandler);
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
            View state_try_again = fragment.inflate(R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load(false));
            fragment.draw(state_try_again);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void invalidate() {
        invalidate(false);
    }

    @Override
    public void invalidate(boolean refresh) {
        if (tab != null) {
            tab.onInvalidate(refresh);
        }
    }

    @Override
    public void setQuery(String query) {
        lastQuery = this.query;
        this.query = query;
    }

    private String getQuery() {
        return query;
    }

    private boolean isSameQueryRequested() {
        return lastQuery != null && lastQuery.equals(query);
    }

    // -->- Attestations global menu ->--

    private void attestationsMenuShare(View view, RVAAttestations entity) {
        thread.runOnUI(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            SparseArray<SSubject> menuSubjectsMap = new SparseArray<>();
            PopupMenu popup = new PopupMenu(activity, view);
            popup.inflate(R.menu.schedule_attestations_common_share);
            if (CollectionUtils.isEmpty(entity.getSubjects())) {
                popup.getMenu().findItem(R.id.share_attestations_schedule).setVisible(false);
            } else {
                Menu subMenu = popup.getMenu().findItem(R.id.share_attestations_schedule).getSubMenu();
                for (SSubject subject : entity.getSubjects()) {
                    int id = View.generateViewId();
                    subMenu.add(Menu.NONE, id, Menu.NONE, subject.getName() + " (" + (subject.getTerm() == 1 ? activity.getString(R.string.term_autumn) : activity.getString(R.string.term_spring)) + ")");
                    menuSubjectsMap.put(id, subject);
                }
            }
            popup.setOnMenuItemClickListener(menuItem -> {
                thread.run(() -> {
                    switch (menuItem.getItemId()) {
                        case R.id.share_all_schedule: shareSchedule(entity.getSubjects()); break;
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

    private void attestationsMenuMore(View view, RVAAttestations entity) {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            boolean isCached = cacheToken != null && StringUtils.isNotBlank(storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken, ""));
            thread.runOnUI(() -> {
                PopupMenu popup = new PopupMenu(activity, view);
                popup.inflate(R.menu.schedule_attestations_common_more);
                popup.getMenu().findItem(R.id.toggle_cache).setChecked(isCached);
                popup.setOnMenuItemClickListener(menuItem -> {
                    thread.run(() -> {
                        switch (menuItem.getItemId()) {
                            case R.id.toggle_cache:
                                if (toggleCache()) {
                                    thread.runOnUI(() -> menuItem.setChecked(!isCached));
                                }
                                break;
                            case R.id.open_settings: activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null); break;
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
        if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken)) {
            if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                return true;
            } else {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return false;
            }
        }
        if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken, schedule.toJsonString())) {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
            return true;
        } else {
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            return false;
        }
    }

    private void shareSchedule(ArrayList<SSubject> subjects) {
        thread.assertNotUI();
        if (schedule == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleAttestations.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        if (CollectionUtils.isEmpty(subjects)) {
            sb.append(activity.getString(R.string.no_attestations));
        } else {
            int examsThisTerm = 0;
            for (SSubject subject : subjects) {
                if (subject == null) {
                    continue;
                }
                examsThisTerm++;
                sb.append("\n");
                shareScheduleAppendSubject(sb, subject);
            }
            if (examsThisTerm == 0) {
                sb.append(activity.getString(R.string.no_attestations));
            }
        }
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_sattest_all"));
    }

    private void shareSchedule(SSubject subject) {
        thread.assertNotUI();
        if (schedule == null || subject == null) {
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(scheduleAttestations.getScheduleHeader(schedule.getTitle(), schedule.getType()));
        sb.append("\n");
        shareScheduleAppendSubject(sb, subject);
        eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_sattest_subject"));
    }

    private void shareScheduleAppendSubject(StringBuilder sb, SSubject subject) {
        if (CollectionUtils.isEmpty(subject.getAttestations())) {
            return;
        }
        sb.append(subject.getName());
        sb.append(" (");
        sb.append(subject.getTerm() == 1 ? activity.getString(R.string.term_autumn) : activity.getString(R.string.term_spring));
        sb.append(")\n");
        for (SAttestation attestation : subject.getAttestations()) {
            sb.append(attestation.getName());
            sb.append(" — ");
            sb.append(attestation.getWeek());
            sb.append("\n");
        }
    }

    // -<-- Attestations global menu --<-

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
