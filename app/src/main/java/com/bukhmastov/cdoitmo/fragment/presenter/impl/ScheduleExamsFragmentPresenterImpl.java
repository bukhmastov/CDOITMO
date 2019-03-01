package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.search.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.adapter.pager.PagerExamsAdapter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import static com.bukhmastov.cdoitmo.util.Thread.SE;

public class ScheduleExamsFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements ScheduleExamsFragmentPresenter, ViewPager.OnPageChangeListener {

    private static final String TAG = "SEFragment";
    private int activeTab = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleExamsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    StoragePref storagePref;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleExamsFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(SE);
        thread.run(SE, () -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            // define query
            String scope = tabHostPresenter.restoreData();
            if (scope == null) {
                scope = scheduleExams.getDefaultScope();
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
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(SE, () -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            TabLayout fixedTabs = activity.findViewById(R.id.fixed_tabs);
            if (fixedTabs != null) {
                fixedTabs.setVisibility(View.GONE);
            }
            if (fragment != null && fragment.toolbar() != null) {
                MenuItem actionSearch = fragment.toolbar().findItem(R.id.action_search);
                if (actionSearch != null && actionSearch.isVisible()) {
                    log.v(TAG, "Hiding actionSearch");
                    actionSearch.setVisible(false);
                    actionSearch.setOnMenuItemClickListener(null);
                }
            }
            thread.standalone(() -> {
                thread.interrupt(SE);
            });
        });
    }

    @Override
    public void onResume() {
        thread.run(SE, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            thread.runOnUI(SE, () -> {
                if (fragment != null && fragment.toolbar() != null) {
                    MenuItem action_search = fragment.toolbar().findItem(R.id.action_search);
                    if (action_search != null && !action_search.isVisible()) {
                        log.v(TAG, "Revealing action_search");
                        action_search.setVisible(true);
                        action_search.setOnMenuItemClickListener(item -> {
                            thread.run(SE, () -> {
                                log.v(TAG, "action_search clicked");
                                eventBus.fire(new OpenActivityEvent(ScheduleExamsSearchActivity.class));
                            });
                            return false;
                        });
                    }
                }
            });
            if (!loaded) {
                loaded = true;
                load();
            }
        });
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
        thread.runOnUI(SE, () -> {
            if (fragment.isNotAddedToActivity()) {
                log.w(TAG, "load | fragment not added to activity");
                return;
            }
            if (activity == null) {
                log.w(TAG, "load | activity is null");
                return;
            }
            final FragmentManager fragmentManager = fragment.getChildFragmentManager();
            thread.run(SE, () -> {
                if (tabHostPresenter.getQuery() == null) {
                    tabHostPresenter.setQuery(scheduleExams.getDefaultScope());
                }
                thread.runOnUI(SE, () -> {
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
                        pager.setAdapter(new PagerExamsAdapter(fragmentManager, activity));
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
                        int activeTabByDefault = Integer.parseInt(storagePref.get(activity, "pref_schedule_exams_type", "0"));
                        tab = fixed_tabs.getTabAt(activeTabByDefault);
                    }
                    if (tab != null) tab.select();
                }, throwable -> {
                    log.exception(throwable);
                    try {
                        failed(activity);
                    } catch (Exception e1) {
                        loaded = false;
                        log.exception(e1);
                    }
                });
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

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getThreadToken() {
        return SE;
    }
}
