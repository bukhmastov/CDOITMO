package com.bukhmastov.cdoitmo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.adapters.PagerLessonsAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class ScheduleLessonsFragment extends ConnectedFragment implements ViewPager.OnPageChangeListener {

    private static final String TAG = "SLFragment";
    private boolean loaded = false;
    private int activeTab = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        // define query
        String scope = ScheduleLessonsTabHostFragment.restoreData();
        if (scope == null) {
            scope = ScheduleLessons.getDefaultScope(activity, ScheduleLessons.TYPE);
        }
        final Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra("action_extra")) {
            String action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null && !action_extra.isEmpty()) {
                intent.removeExtra("action_extra");
                scope = action_extra;
            }
        }
        ScheduleLessonsTabHostFragment.setQuery(scope);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        try {
            final TabLayout fixed_tabs = activity.findViewById(R.id.fixed_tabs);
            if (fixed_tabs != null) {
                fixed_tabs.setVisibility(View.GONE);
            }
            if (activity.toolbar != null) {
                MenuItem action_schedule_lessons_search = activity.toolbar.findItem(R.id.action_schedule_lessons_search);
                if (action_schedule_lessons_search != null && action_schedule_lessons_search.isVisible()) {
                    Log.v(TAG, "Hiding action_schedule_lessons_search");
                    action_schedule_lessons_search.setVisible(false);
                    action_schedule_lessons_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (activity.toolbar != null) {
                MenuItem action_schedule_lessons_search = activity.toolbar.findItem(R.id.action_schedule_lessons_search);
                if (action_schedule_lessons_search != null && !action_schedule_lessons_search.isVisible()) {
                    Log.v(TAG, "Revealing action_schedule_lessons_search");
                    action_schedule_lessons_search.setVisible(true);
                    action_schedule_lessons_search.setOnMenuItemClickListener(item -> {
                        Log.v(TAG, "action_schedule_lessons_search clicked");
                        activity.startActivity(new Intent(activity, ScheduleLessonsSearchActivity.class));
                        return false;
                    });
                }
            }
        } catch (Exception e) {
            Static.error(e);
        }
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public void onPageSelected(int position) {
        activeTab = position;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

    private void load() {
        final FragmentManager fragmentManager = getChildFragmentManager();
        Static.T.runThread(() -> {
            if (activity == null) {
                Log.w(TAG, "load | activity is null");
                return;
            }
            final int week = Static.getWeek(activity);
            if (ScheduleLessonsTabHostFragment.getQuery() == null) {
                ScheduleLessonsTabHostFragment.setQuery(ScheduleLessons.getDefaultScope(activity, ScheduleLessons.TYPE));
            }
            Static.T.runOnUiThread(() -> {
                try {
                    if (activity == null) {
                        Log.w(TAG, "load | activity is null");
                        return;
                    }
                    // setup pager adapter
                    final TabLayout fixed_tabs = activity.findViewById(R.id.fixed_tabs);
                    if (fixed_tabs == null) {
                        loaded = false;
                        return;
                    }
                    fixed_tabs.setVisibility(View.VISIBLE);
                    draw(activity, R.layout.layout_schedule_lessons_tabs);
                    final ViewPager schedule_view = activity.findViewById(R.id.schedule_pager);
                    if (schedule_view != null) {
                        schedule_view.setAdapter(new PagerLessonsAdapter(fragmentManager, activity));
                        schedule_view.addOnPageChangeListener(ScheduleLessonsFragment.this);
                        fixed_tabs.setupWithViewPager(schedule_view);
                    }
                    // select tab
                    TabLayout.Tab tab = null;
                    if (activeTab != -1) {
                        try {
                            tab = fixed_tabs.getTabAt(activeTab);
                        } catch (Exception e) {
                            activeTab = -1;
                        }
                    }
                    if (activeTab == -1) {
                        int activeTabByDefault = Integer.parseInt(Storage.pref.get(activity, "pref_schedule_lessons_week", "-1"));
                        if (activeTabByDefault == -1) {
                            tab = fixed_tabs.getTabAt(week >= 0 ? (week % 2) + 1 : 0);
                        } else {
                            tab = fixed_tabs.getTabAt(activeTabByDefault);
                        }
                    }
                    if (tab != null) tab.select();
                } catch (Exception e) {
                    Static.error(e);
                    try {
                        failed(activity);
                    } catch (Exception e1) {
                        loaded = false;
                        Static.error(e1);
                    }
                }
            });
        });
    }
    private void failed(Activity activity) {
        try {
            if (activity == null) {
                Log.w(TAG, "failed | activity is null");
                return;
            }
            View state_try_again = inflate(activity, R.layout.state_try_again);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load());
            draw(activity, state_try_again);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    private void draw(Activity activity, View view) {
        try {
            ViewGroup vg = activity.findViewById(R.id.container_schedule);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
    private void draw(Activity activity, int layoutId) {
        try {
            draw(activity, inflate(activity, layoutId));
        } catch (Exception e){
            Static.error(e);
        }
    }
    private View inflate(Context context, int layoutId) throws InflateException {
        return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
