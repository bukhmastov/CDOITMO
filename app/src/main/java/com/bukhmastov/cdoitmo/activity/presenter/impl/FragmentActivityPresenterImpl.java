package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.FragmentActivity;
import com.bukhmastov.cdoitmo.activity.presenter.FragmentActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.MainActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import dagger.Lazy;

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
    @Inject
    Lazy<NotificationMessage> notificationMessage;

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
            Class connectedFragmentClass = (Class) extras.get("class");
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
                activity.setSupportActionBar(toolbar);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setHomeButtonEnabled(true);
            }
            if (activity.layoutWithMenu) {
                NavigationView navigationView = activity.findViewById(R.id.nav_view);
                if (navigationView != null) {
                    navigationView.setNavigationItemSelectedListener(this);
                }
            }
            openRootFragment(connectedFragmentClass, fragmentExtras);
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
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

    private void openRootFragment(Class connectedFragmentClass, Bundle extras) {
        thread.runOnUI(() -> {
            log.v(TAG, "openRootFragment | ", connectedFragmentClass);
            if (!activity.openFragment(ConnectedActivity.TYPE.ROOT, connectedFragmentClass, extras)) {
                log.w(TAG, "openRootFragment | ", connectedFragmentClass, " | failed to open fragment");
                notificationMessage.get().snackBar(
                        activity,
                        activity.getString(R.string.failed_to_open_fragment),
                        activity.getString(R.string.redo),
                        view -> openRootFragment(connectedFragmentClass, extras)
                );
            }
        }, throwable -> {
            log.exception(throwable);
            activity.finish();
        });
    }

    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            activity.draw(R.layout.state_failed_text);
            TextView message = activity.findViewById(R.id.text);
            if (message != null) {
                message.setText(R.string.error_occurred);
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }
}
