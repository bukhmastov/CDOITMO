package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.activities.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapters.ScheduleExamsRecyclerViewAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

public class ScheduleExamsFragment extends ConnectedFragment {

    private static final String TAG = "SEFragment";
    public interface TabProvider {
        void onInvalidate(boolean refresh);
    }
    public class Scroll {
        public int position = 0;
        public int offset = 0;
    }
    private boolean loaded = false;
    private ScheduleExams scheduleExams = null;
    private Client.Request requestHandle = null;
    private static String lastQuery = null;
    private static String query = null;
    public static Scroll scroll = null;
    public static TabProvider tab = null;

    private boolean invalidate = false;
    private boolean invalidate_refresh = false;

    public static void setQuery(String query) {
        ScheduleExamsFragment.lastQuery = ScheduleExamsFragment.query;
        ScheduleExamsFragment.query = query;
    }
    public static String getQuery() {
        return ScheduleExamsFragment.query;
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
        ScheduleExamsFragment.setQuery(ScheduleExams.getDefaultScope(activity, ScheduleExams.TYPE));
        final Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra("action_extra")) {
            String action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null && !action_extra.isEmpty()) {
                intent.removeExtra("action_extra");
                ScheduleExamsFragment.setQuery(action_extra);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Fragment destroyed");
        try {
            if (activity.toolbar != null) {
                MenuItem action_schedule_exams_search = activity.toolbar.findItem(R.id.action_schedule_exams_search);
                if (action_schedule_exams_search != null && action_schedule_exams_search.isVisible()) {
                    Log.v(TAG, "Hiding action_schedule_exams_search");
                    action_schedule_exams_search.setVisible(false);
                    action_schedule_exams_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
        tab = null;
        scroll = null;
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_exams, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (activity.toolbar != null && !Static.OFFLINE_MODE) {
                MenuItem action_schedule_exams_search = activity.toolbar.findItem(R.id.action_schedule_exams_search);
                if (action_schedule_exams_search != null && !action_schedule_exams_search.isVisible()) {
                    Log.v(TAG, "Revealing action_schedule_exams_search");
                    action_schedule_exams_search.setVisible(true);
                    action_schedule_exams_search.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Log.v(TAG, "action_schedule_exams_search clicked");
                            startActivity(new Intent(activity, ScheduleExamsSearchActivity.class));
                            return false;
                        }
                    });
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
        if (tab == null) {
            tab = new TabProvider() {
                @Override
                public void onInvalidate(boolean refresh) {
                    Log.v(TAG, "onInvalidate | refresh=" + Static.logBoolean(refresh));
                    if (isResumed()) {
                        invalidate = false;
                        invalidate_refresh = false;
                        load(refresh);
                    } else {
                        invalidate = true;
                        invalidate_refresh = refresh;
                    }
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

    private void load(final boolean refresh) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                draw(R.layout.state_loading);
                Static.T.runThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (getQuery() == null) {
                                throw new NullPointerException("ScheduleExamsFragment.query is null");
                            }
                            if (scroll != null && !isSameQueryRequested()) {
                                scroll.position = 0;
                                scroll.offset = 0;
                            }
                            if (refresh) {
                                getScheduleExams().search(activity, getQuery(), 0);
                            } else {
                                getScheduleExams().search(activity, getQuery());
                            }
                        } catch (Exception e) {
                            Static.error(e);
                            failed();
                        }
                    }
                });
            }
        });
    }
    private @NonNull ScheduleExams getScheduleExams() {
        if (scheduleExams == null) scheduleExams = new ScheduleExams(new Schedule.Handler() {
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
                                        load(false);
                                        return;
                                    }
                                }
                            } catch (Exception ignore) {
                                // ignore
                            }
                            final ScheduleExamsRecyclerViewAdapter adapter = new ScheduleExamsRecyclerViewAdapter(activity, json, new Static.StringCallback() {
                                @Override
                                public void onCall(String data) {
                                    setQuery(data);
                                    load(false);
                                }
                            });
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        draw(R.layout.layout_schedule_both_recycle_list);
                                        // swipe
                                        final SwipeRefreshLayout swipe_container = activity.findViewById(R.id.schedule_swipe);
                                        if (swipe_container != null) {
                                            swipe_container.setColorSchemeColors(Static.colorAccent);
                                            swipe_container.setProgressBackgroundColorSchemeColor(Static.colorBackgroundRefresh);
                                            swipe_container.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                @Override
                                                public void onRefresh() {
                                                    swipe_container.setRefreshing(false);
                                                    load(true);
                                                }
                                            });
                                        }
                                        // recycle view (list)
                                        final RecyclerView schedule_list = activity.findViewById(R.id.schedule_list);
                                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
                                        schedule_list.setLayoutManager(layoutManager);
                                        schedule_list.setAdapter(adapter);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            schedule_list.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                                                @Override
                                                public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                                                    final int position = layoutManager.findFirstVisibleItemPosition();
                                                    final View v = schedule_list.getChildAt(0);
                                                    final int offset = (v == null) ? 0 : (v.getTop() - schedule_list.getPaddingTop());
                                                    if (scroll == null) {
                                                        scroll = new Scroll();
                                                    }
                                                    scroll.position = position;
                                                    scroll.offset = offset;
                                                }
                                            });
                                        }
                                        // scroll to previous position
                                        if (scroll != null) {
                                            layoutManager.scrollToPositionWithOffset(scroll.position, scroll.offset);
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Static.error(e);
                            failed();
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
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.state_offline);
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
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.state_try_again);
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
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.schedule_empty_query);
                                    view.findViewById(R.id.open_settings).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            activity.openActivity(ConnectedActivity.TYPE.stackable, SettingsScheduleExamsFragment.class, null);
                                        }
                                    });
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_NOT_FOUND: {
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.nothing_to_display);
                                    ((TextView) view.findViewById(R.id.ntd_text)).setText(R.string.no_schedule);
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_INVALID_QUERY: {
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.state_failed);
                                    ((TextView) view.findViewById(R.id.text)).setText(R.string.incorrect_query);
                                    draw(view);
                                    break;
                                }
                                case Schedule.FAILED_MINE_NEED_ISU: {
                                    // TODO replace with isu auth, when isu will be ready
                                    final ViewGroup view = (ViewGroup) inflate(R.layout.state_try_again);
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
                            final ViewGroup view = (ViewGroup) inflate(R.layout.state_loading);
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
        return scheduleExams;
    }
    private void failed() {
        try {
            draw(R.layout.state_try_again);
            View try_again_reload = activity.findViewById(R.id.try_again_reload);
            if (try_again_reload != null) {
                try_again_reload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        load(false);
                    }
                });
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void draw(final View view) {
        try {
            ViewGroup vg = activity.findViewById(R.id.container_schedule_exams);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void draw(final int layoutId) {
        try {
            draw(inflate(layoutId));
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
