package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.pager.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.android.material.tabs.TabLayout;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import static com.bukhmastov.cdoitmo.util.Thread.UH;

public class UniversityFragmentPresenterImpl extends ConnectedFragmentPresenterImpl
        implements UniversityFragmentPresenter, ViewPager.OnPageChangeListener {

    private static final String TAG = "UniversityFragment";
    private int tabSelected = -1;
    private String actionExtra = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    NotificationMessage notificationMessage;

    public UniversityFragmentPresenterImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.UNIVERSITY)) {
            return;
        }
        tabSelected = -1;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(UH);
        thread.run(UH, () -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            Intent intent = activity.getIntent();
            if (intent != null) {
                actionExtra = intent.getStringExtra("action_extra");
                if (actionExtra != null) {
                    intent.removeExtra("action_extra");
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(UH, () -> {
            log.v(TAG, "Fragment destroyed");
            TabLayout scrollableTabs = activity.findViewById(R.id.scrollable_tabs);
            if (scrollableTabs != null) {
                scrollableTabs.setVisibility(View.GONE);
            }
            thread.standalone(() -> {
                thread.interrupt(UH);
            });
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(UH, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            TabLayout scrollableTabs = activity.findViewById(R.id.scrollable_tabs);
            if (scrollableTabs != null) {
                scrollableTabs.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        thread.runOnUI(UH, () -> {
            if (fragment.isNotAddedToActivity()) {
                log.w(TAG, "onViewCreated | fragment not added to activity");
                return;
            }
            TabLayout tabs = activity.findViewById(R.id.scrollable_tabs);
            if (tabs == null) {
                return;
            }
            ViewPager pager = fragment.container().findViewById(R.id.pager);
            if (pager == null) {
                return;
            }
            FragmentManager fragmentManager = fragment.getChildFragmentManager();
            pager.setAdapter(new PagerUniversityAdapter(fragmentManager, activity));
            pager.addOnPageChangeListener(this);
            tabs.setupWithViewPager(pager);
            TabLayout.Tab tab = null;
            try {
                if (actionExtra != null) {
                    switch (actionExtra) {
                        case "persons": tab = tabs.getTabAt(0); break;
                        case "faculties": tab = tabs.getTabAt(1); break;
                        case "units": tab = tabs.getTabAt(2); break;
                        case "ubuildings": tab = tabs.getTabAt(3); break;
                        case "news": tab = tabs.getTabAt(4); break;
                        case "events": tab = tabs.getTabAt(5); break;
                    }
                    actionExtra = null;
                }
                if (tab == null) {
                    if (tabSelected == -1) {
                        int pref = storagePref.get(activity, "pref_university_tab", -1);
                        tab = tabs.getTabAt(pref < 0 ? 0 : pref);
                    } else {
                        tab = tabs.getTabAt(tabSelected);
                    }
                }
            } catch (Exception e) {
                tab = null;
            }
            if (tab != null) {
                tab.select();
            }
        });
    }

    @Override
    public void onPageSelected(int position) {
        tabSelected = position;
        storagePref.put(activity, "pref_university_tab", tabSelected);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getThreadToken() {
        return UH;
    }
}
