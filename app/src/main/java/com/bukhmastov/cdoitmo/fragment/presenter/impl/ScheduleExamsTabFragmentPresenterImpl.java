package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleExamsRVA;
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

public class ScheduleExamsTabFragmentPresenterImpl implements ScheduleExamsTabFragmentPresenter {

    private static final String TAG = "SLTabFragment";
    private ConnectedActivity activity = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private View container = null;
    private int type = ScheduleExamsTabHostFragmentPresenter.DEFAULT_INVALID_TYPE;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    ScheduleExamsTabHostFragmentPresenter tabHostPresenter;

    public ScheduleExamsTabFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, ConnectedActivity activity, Fragment fragment) {
        this.activity = activity;
        Bundle bundle = fragment.getArguments();
        if (bundle != null && bundle.containsKey("type")) {
            type = bundle.getInt("type");
        } else {
            log.w(TAG, "onCreate | UNDEFINED TYPE, going to use DEFAULT_TYPE");
            type = ScheduleExamsTabHostFragmentPresenter.DEFAULT_TYPE;
        }
        log.v(TAG, "Fragment created | type=", type);
        tabHostPresenter.tabs().put(type, refresh -> {
            log.v(TAG, "onInvalidate | type=", type, " | refresh=", refresh);
            if (fragment.isResumed()) {
                tabHostPresenter.setInvalidate(false);
                tabHostPresenter.setInvalidateRefresh(false);
                load(refresh);
            } else {
                tabHostPresenter.setInvalidate(true);
                tabHostPresenter.setInvalidateRefresh(refresh);
            }
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed | type=", type);
        tabHostPresenter.tabs().remove(type);
        tabHostPresenter.scroll().remove(type);
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
        log.v(TAG, "Fragment resumed | type=", type, " | loaded=", loaded, " | invalidate=", tabHostPresenter.getInvalidate(), " | invalidate_refresh=", tabHostPresenter.getInvalidateRefresh());
        if (tabHostPresenter.getInvalidate()) {
            tabHostPresenter.setInvalidate(false);
            loaded = true;
            load(tabHostPresenter.getInvalidateRefresh());
            tabHostPresenter.setInvalidateRefresh(false);
        } else if (!loaded) {
            loaded = true;
            load(false);
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused | type=", type);
        if (requestHandle != null && requestHandle.cancel()) {
            log.v(TAG, "Fragment paused | type=", type, " | paused and requested reload");
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
            draw(activity, R.layout.state_loading_text);
            thread.run(() -> {
                try {
                    if (activity == null || tabHostPresenter.getQuery() == null) {
                        log.w(TAG, "load | some values are null | activity=", activity, " | getQuery()=", tabHostPresenter.getQuery());
                        failed(activity);
                        return;
                    }
                    if (tabHostPresenter.scroll() != null && !tabHostPresenter.isSameQueryRequested()) {
                        tabHostPresenter.scroll().clear();
                    }
                    if (refresh) {
                        getScheduleExams(activity).search(activity, tabHostPresenter.getQuery(), 0);
                    } else {
                        getScheduleExams(activity).search(activity, tabHostPresenter.getQuery());
                    }
                } catch (Exception e) {
                    log.exception(e);
                    failed(activity);
                }
            });
        });
    }

    private @NonNull ScheduleExams getScheduleExams(final ConnectedActivity activity) {
        scheduleExams.init(new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                thread.run(() -> {
                    try {
                        try {
                            if (json.getString("type").equals("teachers")) {
                                JSONArray schedule = json.getJSONArray("schedule");
                                if (schedule.length() == 1) {
                                    tabHostPresenter.setQuery(schedule.getJSONObject(0).getString("pid"));
                                    tabHostPresenter.invalidate(false);
                                    return;
                                }
                            }
                        } catch (Exception ignore) {
                            // ignore
                        }
                        // fetch only right exams
                        JSONObject jsonConverted = new JSONObject(json.toString());
                        JSONArray examsConverted = new JSONArray();
                        JSONArray exams = json.getJSONArray("schedule");
                        for (int i = 0; i < exams.length(); i++) {
                            JSONObject exam = exams.getJSONObject(i);
                            String type = exam.has("type") ? exam.getString("type") : "exam";
                            if (ScheduleExamsTabFragmentPresenterImpl.this.type == 0 && "exam".equals(type) || ScheduleExamsTabFragmentPresenterImpl.this.type == 1 && "credit".equals(type)) {
                                examsConverted.put(exam);
                            }
                        }
                        jsonConverted.put("schedule", examsConverted);
                        // get rva adapter
                        final ScheduleExamsRVA adapter = new ScheduleExamsRVA(activity, jsonConverted, type, data -> {
                            tabHostPresenter.setQuery(data);
                            tabHostPresenter.invalidate(false);
                        });
                        thread.runOnUI(() -> {
                            try {
                                draw(activity, R.layout.layout_schedule_both_recycle_list);
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
                                    tabHostPresenter.invalidate(true);
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
                                        ScheduleExamsTabHostFragmentPresenter.Scroll s = tabHostPresenter.scroll().get(type, null);
                                        if (s == null) {
                                            s = new ScheduleExamsTabHostFragmentPresenter.Scroll();
                                        }
                                        s.position = position;
                                        s.offset = offset;
                                        tabHostPresenter.scroll().put(type, s);
                                    });
                                }
                                // scroll to previous position
                                ScheduleExamsTabHostFragmentPresenter.Scroll s = tabHostPresenter.scroll().get(type, null);
                                if (s != null) {
                                    layoutManager.scrollToPositionWithOffset(s.position, s.offset);
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
                            case Schedule.FAILED_MINE_NEED_ISU: {
                                // TODO replace with isu auth, when isu will be ready
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
                                if (view != null) {
                                    view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                    draw(view);
                                }
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
                        log.v(TAG, "onProgress | state=", state);
                        final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading_text);
                        if (view != null) {
                            ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                            draw(view);
                        }
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
        });
        return scheduleExams;
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
            try {
                ViewGroup vg = container.findViewById(R.id.container);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            } catch (Exception e){
                log.exception(e);
            }
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
}
