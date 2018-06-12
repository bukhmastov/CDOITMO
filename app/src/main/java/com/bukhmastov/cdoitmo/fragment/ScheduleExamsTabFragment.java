package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleExamsTabFragment extends ScheduleExamsTabHostFragment {

    private static final String TAG = "SLTabFragment";
    private boolean loaded = false;
    private ScheduleExams scheduleExams = null;
    private Client.Request requestHandle = null;
    private View container = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("type")) {
            TYPE = bundle.getInt("type");
        } else {
            Log.w(TAG, "onCreate | UNDEFINED TYPE, going to use TYPE=", DEFAULT_TYPE);
            TYPE = DEFAULT_TYPE;
        }
        Log.v(TAG, "Fragment created | TYPE=", TYPE);
        tabs.put(TYPE, refresh -> {
            Log.v(TAG, "onInvalidate | TYPE=", TYPE, " | refresh=", refresh);
            if (isResumed()) {
                invalidate = false;
                invalidate_refresh = false;
                load(refresh);
            } else {
                invalidate = true;
                invalidate_refresh = refresh;
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Fragment destroyed | TYPE=", TYPE);
        tabs.remove(TYPE);
        scroll.remove(TYPE);
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.container = inflater.inflate(R.layout.fragment_container, container, false);
        return this.container;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loaded = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed | TYPE=", TYPE, " | loaded=", loaded, " | invalidate=", invalidate, " | invalidate_refresh=", invalidate_refresh);
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
        Log.v(TAG, "paused | TYPE=", TYPE);
        if (requestHandle != null && requestHandle.cancel()) {
            Log.v(TAG, "paused | TYPE=", TYPE, " | paused and requested reload");
            loaded = false;
        }
    }

    private void load(final boolean refresh) {
        Static.T.runOnUiThread(() -> {
            if (activity == null) {
                Log.w(TAG, "load | activity is null");
                failed(getContext());
                return;
            }
            draw(activity, R.layout.state_loading_text);
            Static.T.runThread(() -> {
                try {
                    if (activity == null || getQuery() == null) {
                        Log.w(TAG, "load | some values are null | activity=", activity, " | getQuery()=", getQuery());
                        failed(activity);
                        return;
                    }
                    if (scroll != null && !isSameQueryRequested()) {
                        scroll.clear();
                    }
                    if (refresh) {
                        getScheduleExams(activity).search(activity, getQuery(), 0);
                    } else {
                        getScheduleExams(activity).search(activity, getQuery());
                    }
                } catch (Exception e) {
                    Static.error(e);
                    failed(activity);
                }
            });
        });
    }
    private @NonNull ScheduleExams getScheduleExams(final ConnectedActivity activity) {
        if (scheduleExams == null) scheduleExams = new ScheduleExams(new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                Static.T.runThread(() -> {
                    try {
                        try {
                            if (json.getString("type").equals("teachers")) {
                                JSONArray schedule = json.getJSONArray("schedule");
                                if (schedule.length() == 1) {
                                    setQuery(schedule.getJSONObject(0).getString("pid"));
                                    invalidate(false);
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
                            if (TYPE == 0 && "exam".equals(type) || TYPE == 1 && "credit".equals(type)) {
                                examsConverted.put(exam);
                            }
                        }
                        jsonConverted.put("schedule", examsConverted);
                        // get rva adapter
                        final ScheduleExamsRVA adapter = new ScheduleExamsRVA(activity, jsonConverted, TYPE, data -> {
                            setQuery(data);
                            invalidate(false);
                        });
                        Static.T.runOnUiThread(() -> {
                            try {
                                draw(activity, R.layout.layout_schedule_both_recycle_list);
                                // prepare
                                final SwipeRefreshLayout swipe_container = container.findViewById(R.id.schedule_swipe);
                                final RecyclerView schedule_list = container.findViewById(R.id.schedule_list);
                                if (swipe_container == null || schedule_list == null) throw new SilentException();
                                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                                // swipe
                                swipe_container.setColorSchemeColors(Static.colorAccent);
                                swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                                swipe_container.setOnRefreshListener(() -> {
                                    swipe_container.setRefreshing(false);
                                    invalidate(true);
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
                                        Scroll s = scroll.get(TYPE, null);
                                        if (s == null) {
                                            s = new Scroll();
                                        }
                                        s.position = position;
                                        s.offset = offset;
                                        scroll.put(TYPE, s);
                                    });
                                }
                                // scroll to previous position
                                final Scroll s = scroll.get(TYPE, null);
                                if (s != null) {
                                    layoutManager.scrollToPositionWithOffset(s.position, s.offset);
                                }
                            } catch (SilentException ignore) {
                                failed(activity);
                            } catch (Exception e) {
                                Static.error(e);
                                failed(activity);
                            }
                        });
                    } catch (Exception e) {
                        Static.error(e);
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
                Static.T.runOnUiThread(() -> {
                    try {
                        Log.v(TAG, "onFailure | statusCode=", statusCode, " | state=", state);
                        switch (state) {
                            case Client.FAILED_OFFLINE:
                            case Schedule.FAILED_OFFLINE: {
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_offline_text);
                                view.findViewById(R.id.offline_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                            case Client.FAILED_TRY_AGAIN:
                            case Client.FAILED_SERVER_ERROR:
                            case Client.FAILED_CORRUPTED_JSON:
                            case Schedule.FAILED_LOAD: {
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
                                switch (state) {
                                    case Client.FAILED_SERVER_ERROR:   ((TextView) view.findViewById(R.id.try_again_message)).setText(Client.getFailureMessage(activity, statusCode)); break;
                                    case Client.FAILED_CORRUPTED_JSON: ((TextView) view.findViewById(R.id.try_again_message)).setText(R.string.server_provided_corrupted_json); break;
                                }
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_EMPTY_QUERY: {
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.layout_schedule_empty_query);
                                view.findViewById(R.id.open_settings).setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleExamsFragment.class, null));
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_NOT_FOUND: {
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_nothing_to_display_compact);
                                ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_INVALID_QUERY: {
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_text);
                                ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                draw(view);
                                break;
                            }
                            case Schedule.FAILED_MINE_NEED_ISU: {
                                // TODO replace with isu auth, when isu will be ready
                                final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed_button);
                                view.findViewById(R.id.try_again_reload).setOnClickListener(v -> load(false));
                                draw(view);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Static.error(e);
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                Static.T.runOnUiThread(() -> {
                    try {
                        Log.v(TAG, "onProgress | state=", state);
                        final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading_text);
                        ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                        draw(view);
                    } catch (Exception e) {
                        Static.error(e);
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
                Log.w(TAG, "failed | context is null");
                return;
            }
            View state_try_again = inflate(context, R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load(false));
            draw(state_try_again);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void draw(View view) {
        try {
            ViewGroup vg = container.findViewById(R.id.container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void draw(Context context, int layoutId) {
        try {
            draw(inflate(context, layoutId));
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(Context context, int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
