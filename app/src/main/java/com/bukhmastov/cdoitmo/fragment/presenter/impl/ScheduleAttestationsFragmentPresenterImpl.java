package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleAttestationsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONObject;

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

    private final Schedule.Handler scheduleHandler;
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
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
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleAttestationsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        scheduleHandler = new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                thread.run(() -> {
                    try {
                        final int week = time.getWeek(activity);
                        final ScheduleAttestationsRVA adapter = new ScheduleAttestationsRVA(activity, json, week);
                        thread.runOnUI(() -> {
                            try {
                                fragment.draw(R.layout.layout_schedule_both_recycle_list);
                                // prepare
                                final SwipeRefreshLayout swipe_container = fragment.container().findViewById(R.id.schedule_swipe);
                                final RecyclerView schedule_list = fragment.container().findViewById(R.id.schedule_list);
                                if (swipe_container == null || schedule_list == null) throw new SilentException();
                                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                                // swipe
                                swipe_container.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                                swipe_container.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                                swipe_container.setOnRefreshListener(() -> {
                                    swipe_container.setRefreshing(false);
                                    load(true);
                                });
                                // recycle view (list)
                                schedule_list.setLayoutManager(layoutManager);
                                schedule_list.setAdapter(adapter);
                                schedule_list.setHasFixedSize(true);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    schedule_list.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                                        final int position = layoutManager.findFirstVisibleItemPosition();
                                        final View v = schedule_list.getChildAt(0);
                                        final int offset = (v == null) ? 0 : (v.getTop() - schedule_list.getPaddingTop());
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
                            } catch (SilentException ignore) {
                                failed(activity);
                            } catch (Exception e) {
                                log.exception(e);
                                failed(activity);
                            }
                        });
                    } catch (Exception e) {
                        log.exception(e);
                        failed(activity);
                    }
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                thread.runOnUI(() -> {
                    try {
                        log.v(TAG, "onFailure | statusCode=" + statusCode + " | state=" + state);
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
                    } catch (Exception e) {
                        log.exception(e);
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                thread.runOnUI(() -> {
                    try {
                        log.v(TAG, "onProgress | state=" + state);
                        final ViewGroup view = (ViewGroup) fragment.inflate(R.layout.state_loading_text);
                        ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                        fragment.draw(view);
                    } catch (Exception e) {
                        log.exception(e);
                    }
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
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        // define query
        String scope = fragment.restoreData(fragment);
        if (scope == null) {
            scope = scheduleAttestations.getDefaultScope(activity);
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
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
        tab = null;
        scroll = null;
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && action_search.isVisible()) {
                    log.v(TAG, "Hiding action_search");
                    action_search.setVisible(false);
                    action_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    @Override
    public void onResume() {
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && !action_search.isVisible()) {
                    log.v(TAG, "Revealing action_search");
                    action_search.setVisible(true);
                    action_search.setOnMenuItemClickListener(item -> {
                        log.v(TAG, "action_search clicked");
                        eventBus.fire(new OpenActivityEvent(ScheduleAttestationsSearchActivity.class));
                        return false;
                    });
                }
            }
        } catch (Exception e){
            log.exception(e);
        }
        if (tab == null) {
            tab = refresh -> {
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
            };
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
    }

    @Override
    public void onPause() {
        log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    private void load(final boolean refresh) {
        thread.runOnUI(() -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                failed(activity);
                return;
            }
            fragment.draw(R.layout.state_loading_text);
            thread.run(() -> {
                try {
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
                        scheduleAttestations.search(activity, scheduleHandler, getQuery(), 0);
                    } else {
                        scheduleAttestations.search(activity, scheduleHandler, getQuery());
                    }
                } catch (Exception e) {
                    log.exception(e);
                    failed(activity);
                }
            });
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
        return lastQuery != null && query != null && lastQuery.equals(query);
    }
}
