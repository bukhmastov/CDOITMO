package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.Toolbar;
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
            Intent intent = activity.getIntent();
            if (intent == null) {
                throw new NullPointerException("Intent cannot be null");
            }
            Bundle extras = intent.getExtras();
            if (extras == null) {
                throw new NullPointerException("Intent's extras cannot be null");
            }
            if (!extras.containsKey("class")) {
                throw new IllegalStateException("Intent's extras should contains 'class'");
            }
            Class fragmentClass = (Class) extras.get("class");
            if (!extras.containsKey("extras")) {
                throw new IllegalStateException("Intent's extras should contains 'extras'");
            }
            Bundle fragmentExtras = (Bundle) extras.get("extras");
            if (fragmentExtras != null && fragmentExtras.containsKey(ConnectedActivity.ACTIVITY_WITH_MENU)) {
                activity.layoutWithMenu = fragmentExtras.getBoolean(ConnectedActivity.ACTIVITY_WITH_MENU);
                fragmentExtras.remove(ConnectedActivity.ACTIVITY_WITH_MENU);
            }
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            activity.setContentView(activity.layoutWithMenu ? R.layout.activity_fragment : R.layout.activity_fragment_without_menu);
            Toolbar toolbar = activity.findViewById(R.id.toolbar_fragment);
            if (toolbar != null) {
                theme.applyToolbarTheme(activity, toolbar);
                activity.setActionBar(toolbar);
                activity.getActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getActionBar().setHomeButtonEnabled(true);
            }
            if (activity.layoutWithMenu) {
                NavigationView navigationView = activity.findViewById(R.id.nav_view);
                if (navigationView != null) {
                    navigationView.setNavigationItemSelectedListener(this);
                }
            }
            invoke(fragmentClass, fragmentExtras);
        }, throwable -> {
            log.exception(throwable);
            activity.finish();
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(() -> {
            if (activity.layoutWithMenu) {
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
                activity.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (activity.layoutWithMenu) {
            log.v(TAG, "NavigationItemSelected ", item.getTitle());
            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.closeDrawer(GravityCompat.START);
            }
            eventBus.fire(new MainActivityEvent.MenuSelectedItemChangedEvent(item));
            activity.finish();
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        if (!activity.layoutWithMenu) {
            return activity.back();
        }
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        if (drawerLayout == null) {
            return activity.back();
        }
        int drawerLockMode = drawerLayout.getDrawerLockMode(GravityCompat.START);
        if (drawerLayout.isDrawerVisible(GravityCompat.START)
                && (drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_OPEN)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }
        return activity.back();
    }

    private void invoke(Class connectedFragmentClass, Bundle extras) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        thread.runOnUI(() -> {
            log.v(TAG, "invoke | ", connectedFragmentClass.toString());
            ConnectedFragment.Data data = ConnectedFragment.getData(activity, connectedFragmentClass);
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
                activity.pushFragment(new ConnectedActivity.StackElement(ConnectedActivity.TYPE.ROOT, data.connectedFragmentClass, extras));
            }
        }, throwable -> {
            log.exception(throwable);
            activity.finish();
        });
    }
}
