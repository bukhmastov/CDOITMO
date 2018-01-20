package com.bukhmastov.cdoitmo.fragments;

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
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapters.ScheduleLessonsRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.exceptions.SilentException;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class ScheduleLessonsTabFragment extends ScheduleLessonsTabHostFragment {

    private static final String TAG = "SLTabFragment";
    private boolean loaded = false;
    private ScheduleLessons scheduleLessons = null;
    private Client.Request requestHandle = null;
    private View container = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("type")) {
            TYPE = bundle.getInt("type");
        } else {
            Log.w(TAG, "onCreate | UNDEFINED TYPE, going to use TYPE=" + DEFAULT_TYPE);
            TYPE = DEFAULT_TYPE;
        }
        Log.v(TAG, "Fragment created | TYPE=" + TYPE);
        tabs.put(TYPE, new TabProvider() {
            @Override
            public void onInvalidate(boolean refresh) {
                Log.v(TAG, "onInvalidate | TYPE=" + TYPE + " | refresh=" + Log.lBool(refresh));
                if (isResumed()) {
                    invalidate = false;
                    invalidate_refresh = false;
                    load(refresh);
                } else {
                    invalidate = true;
                    invalidate_refresh = refresh;
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Fragment destroyed | TYPE=" + TYPE);
        tabs.remove(TYPE);
        scroll.remove(TYPE);
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.container = inflater.inflate(R.layout.fragment_tab_schedule_lessons, container, false);
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
        Log.v(TAG, "resumed | TYPE=" + TYPE + " | loaded=" + Log.lBool(loaded) + " | invalidate=" + Log.lBool(invalidate) + " | invalidate_refresh=" + Log.lBool(invalidate_refresh));
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
        Log.v(TAG, "paused | TYPE=" + TYPE);
        if (requestHandle != null && requestHandle.cancel()) {
            Log.v(TAG, "paused | TYPE=" + TYPE + " | paused and requested reload");
            loaded = false;
        }
    }

    private void load(final boolean refresh) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity == null) {
                    Log.w(TAG, "load | activity is null");
                    failed(getContext());
                    return;
                }
                draw(activity, R.layout.state_loading);
                Static.T.runThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (activity == null || getQuery() == null) {
                                Log.w(TAG, "load | some values are null | activity=" + Log.lNull(activity) + " | getQuery()=" + Log.lNull(getQuery()));
                                failed(getContext());
                                return;
                            }
                            if (!isSameQueryRequested()) {
                                scroll.clear();
                            }
                            if (refresh) {
                                getScheduleLessons(activity).search(activity, getQuery(), 0);
                            } else {
                                getScheduleLessons(activity).search(activity, getQuery());
                            }
                        } catch (Exception e) {
                            Static.error(e);
                            failed(getContext());
                        }
                    }
                });
            }
        });
    }
    private @NonNull ScheduleLessons getScheduleLessons(final ConnectedActivity activity) {
        if (scheduleLessons == null) scheduleLessons = new ScheduleLessons(new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                Static.T.runThread(new Runnable() {
                    @Override
                    public void run() {
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
                            final int week = Static.getWeek(activity);
                            final ScheduleLessonsRecyclerViewAdapter adapter = new ScheduleLessonsRecyclerViewAdapter(activity, TYPE, json, week, new Static.StringCallback() {
                                @Override
                                public void onCall(String data) {
                                    setQuery(data);
                                    invalidate(false);
                                }
                            });
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        draw(activity, R.layout.layout_schedule_both_recycle_list);
                                        // prepare
                                        final SwipeRefreshLayout swipe_container = container.findViewById(R.id.schedule_swipe);
                                        final RecyclerView schedule_list = container.findViewById(R.id.schedule_list);
                                        if (swipe_container == null || schedule_list == null) throw new SilentException();
                                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
                                        // swipe
                                        swipe_container.setColorSchemeColors(Static.colorAccent);
                                        swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                                        swipe_container.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                            @Override
                                            public void onRefresh() {
                                                swipe_container.setRefreshing(false);
                                                invalidate(true);
                                            }
                                        });
                                        // recycle view (list)
                                        schedule_list.setLayoutManager(layoutManager);
                                        schedule_list.setAdapter(adapter);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            schedule_list.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                                                @Override
                                                public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
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
                                                }
                                            });
                                        }
                                        // scroll to today's schedule
                                        final Scroll s = scroll.get(TYPE, null);
                                        if (s != null) {
                                            layoutManager.scrollToPositionWithOffset(s.position, s.offset);
                                        } else {
                                            if (Storage.pref.get(activity, "pref_schedule_lessons_scroll_to_day", true)) {
                                                int position = -1;
                                                switch (Static.getCalendar().get(Calendar.DAY_OF_WEEK)) {
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
                                        }
                                    } catch (SilentException ignore) {
                                        failed(activity);
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed(activity);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Static.error(e);
                            failed(activity);
                        }
                    }
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.v(TAG, "onFailure | statusCode=" + statusCode + " | state=" + state);
                            switch (state) {
                                case Client.FAILED_OFFLINE:
                                case Schedule.FAILED_OFFLINE: {
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_offline);
                                    view.findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(false);
                                        }
                                    });
                                    draw(view);
                                    break;
                                }
                                case Client.FAILED_TRY_AGAIN:
                                case Client.FAILED_SERVER_ERROR:
                                case Schedule.FAILED_LOAD: {
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_try_again);
                                    if (state == Client.FAILED_TRY_AGAIN) {
                                        ((TextView) view.findViewById(R.id.try_again_message)).setText(Client.getFailureMessage(activity, statusCode));
                                    }
                                    view.findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(false);
                                        }
                                    });
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_EMPTY_QUERY: {
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.schedule_empty_query);
                                    view.findViewById(R.id.open_settings).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            activity.openActivity(ConnectedActivity.TYPE.stackable, SettingsScheduleLessonsFragment.class, null);
                                        }
                                    });
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_NOT_FOUND: {
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.nothing_to_display);
                                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_INVALID_QUERY: {
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_failed);
                                    ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_MINE_NEED_ISU: {
                                    // TODO replace with isu auth, when isu will be ready
                                    final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_try_again);
                                    view.findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            load(false);
                                        }
                                    });
                                    draw(view);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Static.error(e);
                        }
                    }
                });
            }
            @Override
            public void onProgress(final int state) {
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.v(TAG, "onProgress | state=" + state);
                            final ViewGroup view = (ViewGroup) inflate(activity, R.layout.state_loading);
                            ((TextView) view.findViewById(R.id.loading_message)).setText(R.string.loading);
                            draw(view);
                        } catch (Exception e) {
                            Static.error(e);
                        }
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
        return scheduleLessons;
    }
    private void failed(Context context) {
        try {
            if (context == null) {
                Log.w(TAG, "failed | context is null");
                return;
            }
            View state_try_again = inflate(context, R.layout.state_try_again);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    load(false);
                }
            });
            draw(state_try_again);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void draw(View view) {
        try {
            ViewGroup vg = container.findViewById(R.id.container_schedule_lessons);
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