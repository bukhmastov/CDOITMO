package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.FragmentActivity;
import com.bukhmastov.cdoitmo.activity.presenter.FragmentActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.MainActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class FragmentActivityPresenterImpl implements FragmentActivityPresenter, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FragmentActivity";
    private FragmentActivity activity = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    NavigationMenu navigationMenu;
    @Inject
    Theme theme;
    @Inject
    FirebaseConfigProvider firebaseConfigProvider;

    public FragmentActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.runOnUI(() -> {
            log.i(TAG, "Activity created");
            try {
                final Intent intent = activity.getIntent();
                if (intent == null) {
                    throw new NullPointerException("Intent cannot be null");
                }
                final Bundle extras = intent.getExtras();
                final Class fragment_class;
                final Bundle fragment_extras;
                if (extras != null) {
                    if (extras.containsKey("class")) {
                        fragment_class = (Class) extras.get("class");
                    } else {
                        throw new NullPointerException("Intent's extras should contains 'class'");
                    }
                    if (extras.containsKey("extras")) {
                        fragment_extras = (Bundle) extras.get("extras");
                    } else {
                        throw new NullPointerException("Intent's extras should contains 'extras'");
                    }
                    if (fragment_extras != null && fragment_extras.containsKey(ConnectedActivity.ACTIVITY_WITH_MENU)) {
                        activity.layout_with_menu = fragment_extras.getBoolean(ConnectedActivity.ACTIVITY_WITH_MENU);
                        fragment_extras.remove(ConnectedActivity.ACTIVITY_WITH_MENU);
                    }
                } else {
                    throw new NullPointerException("Intent's extras cannot be null");
                }
                activity.setContentView(activity.layout_with_menu ? R.layout.activity_fragment : R.layout.activity_fragment_without_menu);
                final Toolbar toolbar = activity.findViewById(R.id.toolbar_fragment);
                if (toolbar != null) {
                    theme.applyToolbarTheme(activity, toolbar);
                    activity.setSupportActionBar(toolbar);
                }
                final ActionBar actionBar = activity.getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setHomeButtonEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                if (activity.layout_with_menu) {
                    NavigationView navigationView = activity.findViewById(R.id.nav_view);
                    if (navigationView != null) {
                        navigationView.setNavigationItemSelectedListener(this);
                    }
                }
                invoke(fragment_class, fragment_extras);
            } catch (Exception e) {
                log.exception(e);
                activity.finish();
            }
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(() -> {
            if (activity.layout_with_menu) {
                NavigationView navigationView = activity.findViewById(R.id.nav_view);
                navigationMenu.displayEnableDisableOfflineButton(navigationView);
                navigationMenu.hideIfUnauthorizedMode(navigationView);
                navigationMenu.displayUserData(activity, storage, navigationView);
                navigationMenu.displayRemoteMessage(activity, firebaseConfigProvider, storage);
            }
        });
    }

    @Override
    public void onPause() {}

    @Override
    public void onToolbarSetup() {
        navigationMenu.toggleOfflineIcon(activity.toolbar);
    }

    @Override
    public boolean onToolbarSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode:
                eventBus.fire(new MainActivityEvent.SwitchToOfflineModeEvent());
            case android.R.id.home:
                activity.finish();
                return false;
            default:
                return true;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (activity.layout_with_menu) {
            log.v(TAG, "NavigationItemSelected " + item.getTitle());
            DrawerLayout drawer_layout = activity.findViewById(R.id.drawer_layout);
            if (drawer_layout != null) {
                drawer_layout.closeDrawer(GravityCompat.START);
            }
            eventBus.fire(new MainActivityEvent.MenuSelectedItemChangedEvent(item));
            activity.finish();
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        try {
            if (!activity.layout_with_menu) {
                throw new Exception("");
            }
            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            if (drawer == null) {
                throw new Exception("");
            } else {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    throw new Exception("");
                }
            }
        } catch (Exception e) {
            if (activity.back()) {
                return true;
            }
        }
        return false;
    }

    private void invoke(final Class connectedFragmentClass, final Bundle extras) {
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        thread.runOnUI(() -> {
            log.v(TAG, "invoke | " + connectedFragmentClass.toString());
            try {
                ConnectedFragment.Data data = ConnectedFragment.getData(activity, connectedFragmentClass);
                if (data == null) {
                    throw new NullPointerException("data cannot be null");
                }
                activity.updateToolbar(activity, data.title, null);
                ViewGroup root = activity.findViewById(activity.getRootViewId());
                if (root != null) {
                    root.removeAllViews();
                }
                Fragment fragment = (Fragment) data.connectedFragmentClass.newInstance();
                if (extras != null) {
                    fragment.setArguments(extras);
                }
                if (fragmentManager != null) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(activity.getRootViewId(), fragment);
                    fragmentTransaction.commitAllowingStateLoss();
                    activity.pushFragment(new ConnectedActivity.StackElement(ConnectedActivity.TYPE.ROOT, connectedFragmentClass, extras));
                }
            } catch (Exception e) {
                log.exception(e);
                activity.finish();
            }
        });
    }
}
