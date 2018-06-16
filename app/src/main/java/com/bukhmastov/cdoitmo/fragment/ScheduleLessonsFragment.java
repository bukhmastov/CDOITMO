package com.bukhmastov.cdoitmo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.PagerLessonsAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

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
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && action_search.isVisible()) {
                    Log.v(TAG, "Hiding action_search");
                    action_search.setVisible(false);
                    action_search.setOnMenuItemClickListener(null);
                }
            }
        } catch (Exception e){
            Static.error(e);
        }
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
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
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
                    draw(R.layout.fragment_pager);
                    final ViewPager pager = container.findViewById(R.id.pager);
                    if (pager != null) {
                        pager.setAdapter(new PagerLessonsAdapter(fragmentManager, activity));
                        pager.addOnPageChangeListener(ScheduleLessonsFragment.this);
                        fixed_tabs.setupWithViewPager(pager);
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
            View state_try_again = inflate(R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load());
            draw(state_try_again);
        } catch (Exception e) {
            Static.error(e);
        }
    }
}
