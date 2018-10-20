package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleAttestationsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleAttestationsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleAttestationsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
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
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

public class ScheduleAttestationsFragmentPresenterImpl implements ScheduleAttestationsFragmentPresenter {

    private static final String TAG = "SAFragment";

    private interface TabProvider {
        void onInvalidate(boolean refresh);
    }
    private class Scroll {
        public int position = 0;
        public int offset = 0;
    }

    private final Schedule.Handler<SAttestations> scheduleHandler;
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private SAttestations schedule = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
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
        AppComponentProvider.getComponent().inject(this);
        scheduleHandler = new Schedule.Handler<SAttestations>() {
            @Override
            public void onSuccess(final SAttestations schedule, final boolean fromCache) {
                thread.run(() -> {
                    ScheduleAttestationsFragmentPresenterImpl.this.schedule = schedule;
                    int week = time.getWeek(activity);
                    ScheduleAttestationsRVA adapter = new ScheduleAttestationsRVA(schedule, week);
                    adapter.setClickListener(R.id.schedule_lessons_menu, ScheduleAttestationsFragmentPresenterImpl.this::attestationsMenu);
                    thread.runOnUI(() -> {
                        fragment.draw(R.layout.layout_schedule_both_recycle_list);
                        // prepare
                        SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.schedule_swipe);
                        RecyclerView recyclerView = fragment.container().findViewById(R.id.schedule_list);
                        if (swipe == null || recyclerView == null) {
                            return;
                        }
                        LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
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
                        case Schedule.FAILED_MINE_NEED_ISU: {
                            // TODO replace with isu auth, when isu will be ready
                            final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_failed_button);
                            view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
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
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            // define query
            String scope = fragment.restoreData(fragment);
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
            fragment.storeData(fragment, scope);
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            tab = null;
            scroll = null;
            if (activity != null && activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
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
                if (activity != null && activity.toolbar != null) {
                    MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
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
                    fragment.storeData(fragment, getQuery());
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

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
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

    private void attestationsMenu(View view, SSubject entity) {
        thread.run(() -> {
            if (schedule == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                return;
            }
            String cacheToken = schedule.getQuery() == null ? null : schedule.getQuery().toLowerCase();
            boolean isCached = cacheToken != null && StringUtils.isNotBlank(storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken, ""));
            thread.runOnUI(() -> {
                PopupMenu popup = new PopupMenu(activity, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.schedule_attestations, menu);
                menu.findItem(isCached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                popup.setOnMenuItemClickListener(item -> {
                    attestationsMenuSelected(item);
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

    private void attestationsMenuSelected(MenuItem item) {
        log.v(TAG, "Attestations menu | popup item | clicked | " + item.getTitle().toString());
        switch (item.getItemId()) {
            case R.id.add_to_cache:
            case R.id.remove_from_cache: toggleCache(); break;
            case R.id.open_settings: activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null); break;
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
            if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken)) {
                if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken)) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                } else {
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                }
                return;
            }
            if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations#lessons#" + cacheToken, schedule.toJsonString())) {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
            } else {
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
        });
    }

    // -<-- Attestations global menu --<-
}
