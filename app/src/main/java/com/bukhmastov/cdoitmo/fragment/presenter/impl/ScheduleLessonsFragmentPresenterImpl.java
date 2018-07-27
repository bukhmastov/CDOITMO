package com.bukhmastov.cdoitmo.fragment.presenter.impl;

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
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.PagerLessonsAdapter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

public class ScheduleLessonsFragmentPresenterImpl implements ScheduleLessonsFragmentPresenter, ViewPager.OnPageChangeListener {

    private static final String TAG = "SLFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private boolean loaded = false;
    private int activeTab = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleLessonsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    StoragePref storagePref;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleLessonsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
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
        String scope = tabHostPresenter.restoreData();
        if (scope == null) {
            scope = scheduleLessons.getDefaultScope(activity);
        }
        final Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra("action_extra")) {
            String action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null && !action_extra.isEmpty()) {
                intent.removeExtra("action_extra");
                scope = action_extra;
            }
        }
        tabHostPresenter.setQuery(scope);
    }

    @Override
    public void onDestroy() {
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
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        try {
            if (activity.toolbar != null) {
                MenuItem action_search = activity.toolbar.findItem(R.id.action_search);
                if (action_search != null && !action_search.isVisible()) {
                    log.v(TAG, "Revealing action_search");
                    action_search.setVisible(true);
                    action_search.setOnMenuItemClickListener(item -> {
                        log.v(TAG, "action_search clicked");
                        eventBus.fire(new OpenActivityEvent(ScheduleLessonsSearchActivity.class));
                        return false;
                    });
                }
            }
        } catch (Exception e) {
            log.exception(e);
        }
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
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
        final FragmentManager fragmentManager = fragment.getChildFragmentManager();
        thread.run(() -> {
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                return;
            }
            final int week = time.getWeek(activity);
            if (tabHostPresenter.getQuery() == null) {
                tabHostPresenter.setQuery(scheduleLessons.getDefaultScope(activity));
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
                    fragment.draw(R.layout.fragment_pager);
                    final ViewPager pager = fragment.container().findViewById(R.id.pager);
                    if (pager != null) {
                        pager.setAdapter(new PagerLessonsAdapter(fragmentManager, activity));
                        pager.addOnPageChangeListener(this);
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
                        int activeTabByDefault = Integer.parseInt(storagePref.get(activity, "pref_schedule_lessons_week", "-1"));
                        if (activeTabByDefault == -1) {
                            tab = fixed_tabs.getTabAt(week >= 0 ? (week % 2) + 1 : 0);
                        } else {
                            tab = fixed_tabs.getTabAt(activeTabByDefault);
                        }
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
            View state_try_again = fragment.inflate(R.layout.state_failed_button);
            state_try_again.findViewById(R.id.try_again_reload).setOnClickListener(view -> load());
            fragment.draw(state_try_again);
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
