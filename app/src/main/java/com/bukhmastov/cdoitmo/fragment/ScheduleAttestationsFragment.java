package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleAttestationsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleAttestationsRVA;
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.Color;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONObject;

public class ScheduleAttestationsFragment extends ConnectedFragment {

    private static final String TAG = "SAFragment";
    public interface TabProvider {
        void onInvalidate(boolean refresh);
    }
    public class Scroll {
        public int position = 0;
        public int offset = 0;
    }
    private boolean loaded = false;
    private ScheduleAttestations scheduleAttestations = null;
    private Client.Request requestHandle = null;
    private static String lastQuery = null;
    private static String query = null;
    public static Scroll scroll = null;
    public static TabProvider tab = null;

    private boolean invalidate = false;
    private boolean invalidate_refresh = false;

    public static void setQuery(String query) {
        ScheduleAttestationsFragment.lastQuery = ScheduleAttestationsFragment.query;
        ScheduleAttestationsFragment.query = query;
    }
    public static String getQuery() {
        return ScheduleAttestationsFragment.query;
    }
    public static boolean isSameQueryRequested() {
        return lastQuery != null && query != null && lastQuery.equals(query);
    }
    public static void invalidate() {
        invalidate(false);
    }
    public static void invalidate(boolean refresh) {
        if (tab != null) {
            tab.onInvalidate(refresh);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        // define query
        String scope = restoreData(this);
        if (scope == null) {
            scope = ScheduleAttestations.getDefaultScope(activity, ScheduleAttestations.TYPE);
        }
        final Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra("action_extra")) {
            String action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null && !action_extra.isEmpty()) {
                intent.removeExtra("action_extra");
                scope = action_extra;
            }
        }
        ScheduleAttestationsFragment.setQuery(scope);
        storeData(this, scope);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Fragment destroyed");
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && action_search.isVisible()) {
                    Log.v(TAG, "Hiding action_search");
                    action_search.setVisible(false);
                    action_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            Log.exception(e);
        }
        tab = null;
        scroll = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && !action_search.isVisible()) {
                    Log.v(TAG, "Revealing action_search");
                    action_search.setVisible(true);
                    action_search.setOnMenuItemClickListener(item -> {
                        Log.v(TAG, "action_search clicked");
                        activity.startActivity(new Intent(activity, ScheduleAttestationsSearchActivity.class));
                        return false;
                    });
                }
            }
        } catch (Exception e){
            Log.exception(e);
        }
        if (tab == null) {
            tab = refresh -> {
                Log.v(TAG, "onInvalidate | refresh=", refresh);
                storeData(this, getQuery());
                if (isResumed()) {
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
        super.onPause();
        Log.v(TAG, "paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }

    private void load(final boolean refresh) {
        Thread.runOnUI(() -> {
            if (activity == null) {
                Log.w(TAG, "load | activity is null");
                failed(getContext());
                return;
            }
            draw(R.layout.state_loading_text);
            Thread.run(() -> {
                try {
                    if (activity == null || getQuery() == null) {
                        Log.w(TAG, "load | some values are null | activity=" + Log.lNull(activity) + " | getQuery()=" + Log.lNull(getQuery()));
                        failed(getContext());
                        return;
                    }
                    if (scroll != null && !isSameQueryRequested()) {
                        scroll.position = 0;
                        scroll.offset = 0;
                    }
                    if (refresh) {
                        getScheduleAttestations(activity).search(activity, getQuery(), 0);
                    } else {
                        getScheduleAttestations(activity).search(activity, getQuery());
                    }
                } catch (Exception e) {
                    Log.exception(e);
                    failed(activity);
                }
            });
        });
    }
    private @NonNull ScheduleAttestations getScheduleAttestations(final ConnectedActivity activity) {
        if (scheduleAttestations == null) scheduleAttestations = new ScheduleAttestations(new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                Thread.run(() -> {
                    try {
                        final int week = Time.getWeek(activity);
                        final ScheduleAttestationsRVA adapter = new ScheduleAttestationsRVA(activity, json, week);
                        Thread.runOnUI(() -> {
                            try {
                                draw(R.layout.layout_schedule_both_recycle_list);
                                // prepare
                                final SwipeRefreshLayout swipe_container = container.findViewById(R.id.schedule_swipe);
                                final RecyclerView schedule_list = container.findViewById(R.id.schedule_list);
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
                                Log.exception(e);
                                failed(activity);
                            }
                        });
                    } catch (Exception e) {
                        Log.exception(e);
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
                Thread.runOnUI(() -> {
                    try {
                        Log.v(TAG, "onFailure | statusCode=" + statusCode + " | state=" + state);
                        switch (state) {
                            case Client.FAILED_OFFLINE:
                            case Schedule.FAILED_OFFLINE: {
                                final ViewGroup view = (ViewGroup) inflate(R.layout.state_offline_text);
                                view.findViewById(R.id.offline_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                            case Client.FAILED_TRY_AGAIN:
                            case Client.FAILED_SERVER_ERROR:
                            case Schedule.FAILED_LOAD: {
                                final ViewGroup view = (ViewGroup) inflate(R.layout.state_failed_button);
                                if (state == Client.FAILED_TRY_AGAIN) {
                                    ((TextView) view.findViewById(R.id.try_again_message)).setText(Client.getFailureMessage(activity, statusCode));
                                }
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_EMPTY_QUERY: {
                                final ViewGroup view = (ViewGroup) inflate(R.layout.layout_schedule_empty_query);
                                view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleAttestationsFragment.class, null));
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_NOT_FOUND: {
                                final ViewGroup view = (ViewGroup) inflate(R.layout.state_nothing_to_display_compact);
                                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_INVALID_QUERY: {
                                final ViewGroup view = (ViewGroup) inflate(R.layout.state_failed_text);
                                ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_MINE_NEED_ISU: {
                                // TODO replace with isu auth, when isu will be ready
                                final ViewGroup view = (ViewGroup) inflate(R.layout.state_failed_button);
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.exception(e);
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                Thread.runOnUI(() -> {
                    try {
                        Log.v(TAG, "onProgress | state=" + state);
                        final ViewGroup view = (ViewGroup) inflate(R.layout.state_loading_text);
                        ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                        draw(view);
                    } catch (Exception e) {
                        Log.exception(e);
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
        });
        return scheduleAttestations;
    }
    private void failed(Context context) {
        try {
            if (context == null) {
                Log.w(TAG, "failed | context is null");
                return;
            }
            View state_try_again = inflate(R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load(false));
            draw(state_try_again);
        } catch (Exception e) {
            Log.exception(e);
        }
    }
}
