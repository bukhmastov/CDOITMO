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
import com.bukhmastov.cdoitmo.activity.search.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.PagerExamsAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class ScheduleExamsFragment extends ConnectedFragment implements ViewPager.OnPageChangeListener {

    private static final String TAG = "SEFragment";
    private boolean loaded = false;
    private int activeTab = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    StoragePref storagePref;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
        // define query
        String scope = ScheduleExamsTabHostFragment.restoreData();
        if (scope == null) {
            scope = scheduleExams.getDefaultScope(activity);
        }
        final Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra("action_extra")) {
            String action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null && !action_extra.isEmpty()) {
                intent.removeExtra("action_extra");
                scope = action_extra;
            }
        }
        ScheduleExamsTabHostFragment.setQuery(scope);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.v(TAG, "Fragment destroyed");
        try {
            final TabLayout fixed_tabs = activity.findViewById(R.id.fixed_tabs);
            if (fixed_tabs != null) {
                fixed_tabs.setVisibility(View.GONE);
            }
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
        super.onResume();
        log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && !action_search.isVisible()) {
                    log.v(TAG, "Revealing action_search");
                    action_search.setVisible(true);
                    action_search.setOnMenuItemClickListener(item -> {
                        log.v(TAG, "action_search clicked");
                        activity.startActivity(new Intent(activity, ScheduleExamsSearchActivity.class));
                        return false;
                    });
                }
            }
        } catch (Exception e){
            log.exception(e);
        }
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        log.v(TAG, "paused");
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
        thread.run(() -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                return;
            }
            if (ScheduleLessonsTabHostFragment.getQuery() == null) {
                ScheduleLessonsTabHostFragment.setQuery(scheduleExams.getDefaultScope(activity));
            }
            thread.runOnUI(() -> {
                try {
                    if (activity == null) {
                        log.w(TAG, "load | activity is null");
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
                        pager.setAdapter(new PagerExamsAdapter(fragmentManager, activity));
                        pager.addOnPageChangeListener(ScheduleExamsFragment.this);
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
                        int activeTabByDefault = Integer.parseInt(storagePref.get(activity, "pref_schedule_exams_type", "0"));
                        tab = fixed_tabs.getTabAt(activeTabByDefault);
                    }
                    if (tab != null) tab.select();
                } catch (Exception e) {
                    log.exception(e);
                    try {
                        failed(activity);
                    } catch (Exception e1) {
                        loaded = false;
                        log.exception(e1);
                    }
                }
            });
        });
    }
    private void failed(Activity activity) {
        try {
            if (activity == null) {
                log.w(TAG, "failed | activity is null");
                return;
            }
            View state_try_again = inflate(R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load());
            draw(state_try_again);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
