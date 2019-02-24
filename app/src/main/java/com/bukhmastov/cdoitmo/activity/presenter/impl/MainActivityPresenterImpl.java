package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.LoginActivity;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.presenter.MainActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.MainActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.UserInfoChangedEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.fragment.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragment.HomeScreenInteractionFragment;
import com.bukhmastov.cdoitmo.fragment.IsuGroupInfoFragment;
import com.bukhmastov.cdoitmo.fragment.IsuScholarshipPaidFragment;
import com.bukhmastov.cdoitmo.fragment.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragment.RatingFragment;
import com.bukhmastov.cdoitmo.fragment.Room101Fragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Migration;
import com.google.android.material.navigation.NavigationView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import static com.bukhmastov.cdoitmo.util.Thread.AM;

public class MainActivityPresenterImpl implements MainActivityPresenter, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_SELECTION = "selectedSection";
    private MainActivity activity = null;
    private boolean initialized = false;
    private boolean loaded = false;
    private boolean exitOfflineMode = false;
    private int selectedSection = -1;
    private MenuItem selectedMenuItem = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    Storage storage;
    @Inject
    ProtocolTracker protocolTracker;
    @Inject
    Account account;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    NavigationMenu navigationMenu;
    @Inject
    Theme theme;
    @Inject
    InjectProvider injectProvider;
    @Inject
    FirebaseCrashlyticsProvider firebaseCrashlyticsProvider;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebasePerformanceProvider firebasePerformanceProvider;
    @Inject
    FirebaseConfigProvider firebaseConfigProvider;

    public MainActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onMenuSelectedItemChangedEvent(MainActivityEvent.MenuSelectedItemChangedEvent event) {
        selectedMenuItem = event.getSelectedMenuItem();
    }

    @Event
    public void onSwitchToOfflineModeEvent(MainActivityEvent.SwitchToOfflineModeEvent event) {
        exitOfflineMode = true;
    }

    @Event
    public void onUnloadEvent(MainActivityEvent.UnloadEvent event) {
        loaded = false;
    }

    @Event
    public void onUserInfoChangedEvent(UserInfoChangedEvent event) {
        if (!loaded) {
            return;
        }
        thread.run(AM, () -> {
            navigationMenu.displayUserData(activity, storage, activity.findViewById(R.id.nav_view));
        });
    }

    @Override
    public void setActivity(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(AM);
        thread.runOnUI(AM, () -> {
            if (!initialized) {
                log.i(TAG, "Activity created, initialization");
                init(activity);
            } else {
                log.i(TAG, "Activity created, setup");
                setup(activity, savedInstanceState);
            }
        });
    }

    @Override
    public void onResume() {
        thread.runOnUI(AM, () -> {
            log.v(TAG, "Activity resumed");
            if (!initialized) {
                return;
            }
            NavigationView navigationView = activity.findViewById(R.id.nav_view);
            navigationMenu.displayEnableDisableOfflineButton(navigationView);
            navigationMenu.hideIfUnauthorizedMode(navigationView);
            if (!exitOfflineMode && (App.OFFLINE_MODE || account.isAuthorized())) {
                authorized();
            } else {
                authorize(LoginActivity.SIGNAL_LOGIN);
            }
        });
    }

    @Override
    public void onPause() {
        log.v(TAG, "Activity paused");
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
        loaded = false;
        thread.interrupt(AM);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (initialized && selectedSection != -1) {
            savedInstanceState.putInt(STATE_SELECTED_SELECTION, selectedSection);
        }
    }

    @Override
    public void onToolbarSetup() {
        notificationMessage.snackBarOffline(activity);
        navigationMenu.toggleOfflineIcon(activity.toolbar);
    }

    @Override
    public boolean onToolbarSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode: reconnect(); return true;
            case android.R.id.home: toggleDrawer(); return true;
            default: return false;
        }
    }

    @Override
    public boolean onBackButtonPressed() {
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        log.v(TAG, "NavigationItemSelected | item=", item.getTitle());
        DrawerLayout drawer_layout = activity.findViewById(R.id.drawer_layout);
        if (drawer_layout != null) drawer_layout.closeDrawer(GravityCompat.START);
        selectSection(item.getItemId());
        return true;
    }

    @Override
    public void authorize(int state) {
        thread.run(AM, () -> {
            try {
                log.v(TAG, "authorize | state=", state);
                loaded = false;
                exitOfflineMode = false;
                Bundle bundle = new Bundle();
                bundle.putInt("state", state);
                eventBus.fire(new OpenActivityEvent(LoginActivity.class, bundle));
            } catch (Exception ignore) {/* ignore */}
        });
    }

    @Override
    public void authorized() {
        thread.run(AM, () -> {
            log.v(TAG, "authorized");
            if (!loaded) {
                loaded = true;
                thread.standalone(() -> protocolTracker.check(activity));
                selectSection(selectedSection);
                navigationMenu.displayUserData(activity, storage, activity.findViewById(R.id.nav_view));
                navigationMenu.displayRemoteMessage(activity, firebaseConfigProvider, storage);
                thread.runOnUI(AM, () -> {
                    navigationMenu.toggleOfflineIcon(activity.toolbar);
                    notificationMessage.snackBarOffline(activity);
                });
            } else if (selectedMenuItem != null) {
                try {
                    selectSection(selectedMenuItem.getItemId());
                } finally {
                    selectedMenuItem = null;
                }
            }
        });
    }

    @Override
    public void selectSection(int s) {
        final int section = applySectionUnAuthorizedMode(s);
        thread.run(AM, () -> {
            log.v(TAG, "selectSection | section=", section);
            switch (section) {
                case R.id.nav_e_register:
                case R.id.nav_protocol_changes:
                case R.id.nav_rating:
                case R.id.nav_schedule:
                case R.id.nav_schedule_exams:
                case R.id.nav_schedule_attestations:
                case R.id.nav_room101:
                case R.id.nav_group:
                case R.id.nav_scholarship:
                case R.id.nav_university: {
                    final Class connectedFragmentClass;
                    switch (section) {
                        default:
                        case R.id.nav_e_register: connectedFragmentClass = ERegisterFragment.class; break;
                        case R.id.nav_protocol_changes: connectedFragmentClass = ProtocolFragment.class; break;
                        case R.id.nav_rating: connectedFragmentClass = RatingFragment.class; break;
                        case R.id.nav_schedule: connectedFragmentClass = ScheduleLessonsFragment.class; break;
                        case R.id.nav_schedule_exams: connectedFragmentClass = ScheduleExamsFragment.class; break;
                        case R.id.nav_schedule_attestations: connectedFragmentClass = ScheduleAttestationsFragment.class; break;
                        case R.id.nav_room101: connectedFragmentClass = Room101Fragment.class; break;
                        case R.id.nav_group: connectedFragmentClass = IsuGroupInfoFragment.class; break;
                        case R.id.nav_scholarship: connectedFragmentClass = IsuScholarshipPaidFragment.class; break;
                        case R.id.nav_university: connectedFragmentClass = UniversityFragment.class; break;
                    }
                    thread.runOnUI(AM, () -> {
                        if (activity.openFragment(ConnectedActivity.TYPE.ROOT, connectedFragmentClass, null)) {
                            ((NavigationView) activity.findViewById(R.id.nav_view)).setCheckedItem(section);
                            selectedSection = section;
                        } else {
                            notificationMessage.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), view -> selectSection(section));
                        }
                    });
                    break;
                }
                case R.id.nav_homescreen:
                case R.id.nav_settings:
                    final Class connectedFragmentClass;
                    switch (section) {
                        default:
                        case R.id.nav_homescreen: connectedFragmentClass = HomeScreenInteractionFragment.class; break;
                        case R.id.nav_settings: connectedFragmentClass = SettingsFragment.class; break;
                    }
                    thread.runOnUI(AM, () -> {
                        if (activity.openActivityOrFragment(ConnectedActivity.TYPE.ROOT, connectedFragmentClass, null)) {
                            if (App.tablet) {
                                ((NavigationView) activity.findViewById(R.id.nav_view)).setCheckedItem(section);
                                selectedSection = section;
                            }
                        } else {
                            notificationMessage.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), view -> selectSection(section));
                        }
                    });
                    break;
                case R.id.nav_enable_offline_mode:
                case R.id.nav_disable_offline_mode:
                case R.id.nav_change_account:
                case R.id.nav_do_clean_auth:
                case R.id.nav_logout: {
                    loaded = false;
                    switch (section) {
                        case R.id.nav_change_account:
                        case R.id.nav_logout:
                            ConnectedActivity.clearStore();
                            eventBus.fire(new ClearCacheEvent(ClearCacheEvent.ALL));
                            break;
                    }
                    switch (section) {
                        case R.id.nav_enable_offline_mode: authorize(LoginActivity.SIGNAL_GO_OFFLINE); break;
                        case R.id.nav_disable_offline_mode: authorize(LoginActivity.SIGNAL_RECONNECT); break;
                        case R.id.nav_change_account: authorize(LoginActivity.SIGNAL_CHANGE_ACCOUNT); break;
                        case R.id.nav_do_clean_auth: authorize(LoginActivity.SIGNAL_DO_CLEAN_AUTH); break;
                        case R.id.nav_logout: account.logoutConfirmation(activity, () -> authorize(LoginActivity.SIGNAL_LOGOUT)); break;
                    }
                    break;
                }
            }
        });
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    private void init(@NonNull ConnectedActivity activity) {
        thread.run(AM, () -> {
            try {
                try {
                    log.i(TAG, "App | launched");
                    PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                    log.i(TAG, "App | version code = ", pInfo.versionCode);
                    log.i(TAG, "App | sdk = ", Build.VERSION.SDK_INT);
                    log.i(TAG, "App | theme = ", theme.getAppTheme(activity));
                } catch (Exception e) {
                    log.exception(e);
                }
                theme.applyActivityTheme(activity);
                // apply compatibility changes
                Migration.migrate(activity, injectProvider);
                // set default preferences
                storagePref.resetIfNeeded(activity);
                // enable/disable firebase
                firebaseCrashlyticsProvider.setEnabled(activity);
                firebaseAnalyticsProvider.setEnabled(activity);
                firebasePerformanceProvider.setEnabled(activity);
                // set auto_logout value
                eventBus.fire(new MainActivityEvent.AutoLogoutChangedEvent(storagePref.get(activity, "pref_auto_logout", false)));
                // set first_launch and intro values
                App.isFirstLaunchEver = storagePref.get(activity, "pref_first_launch", true);
                App.showIntroducingActivity = App.isFirstLaunchEver;
                if (App.isFirstLaunchEver) {
                    storagePref.put(activity, "pref_first_launch", false);
                }
                // firebase events and properties
                firebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.APP_OPEN);
                firebaseAnalyticsProvider.setUserProperty(activity, FirebaseAnalyticsProvider.Property.THEME, theme.getAppTheme(activity));
            } catch (Exception e) {
                log.exception(e);
            } finally {
                log.i(TAG, "App | initialized");
                thread.runOnUI(AM, () -> {
                    // app initialization completed, going to recreate activity
                    initialized = true;
                    activity.recreate();
                });
            }
        });
    }

    private void setup(@NonNull ConnectedActivity activity, @Nullable Bundle savedInstanceState) {
        thread.runOnUI(AM, () -> {
            // setup toolbar and drawer layout
            Toolbar toolbar = activity.findViewById(R.id.toolbar_main);
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            if (toolbar != null) {
                theme.applyToolbarTheme(activity, toolbar);
                activity.setSupportActionBar(toolbar);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setHomeButtonEnabled(true);
            }
            if (drawerLayout != null) {
                App.tablet = false;
                ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                drawerLayout.addDrawerListener(mDrawerToggle);
                mDrawerToggle.syncState();
                if (App.isFirstLaunchEver) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            } else {
                App.tablet = true;
            }
            NavigationView navigationView = activity.findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
            // track to firebase
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            firebaseAnalyticsProvider.setUserProperty(activity, FirebaseAnalyticsProvider.Property.DEVICE, App.tablet ? "tablet" : "mobile");
            // setup static variables
            App.OFFLINE_MODE = "offline".equals(activity.getIntent().getStringExtra("mode")) ||
                    Client.isOffline(activity) ||
                    (App.firstLaunch && storagePref.get(activity, "pref_initial_offline", false));
            App.firstLaunch = false;
            App.isFirstLaunchEver = false;
            // do some logging
            log.i(TAG, "Device = ", (App.tablet ? "tablet" : "mobile"));
            log.i(TAG, "Mode = ", (App.OFFLINE_MODE ? "offline" : "online"));
            // define section to be opened
            final String action = activity.getIntent().getStringExtra("action");
            if (action == null && savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_SELECTION)) {
                selectedSection = savedInstanceState.getInt(STATE_SELECTED_SELECTION);
                log.v(TAG, "Section selected from savedInstanceState");
            } else {
                String act = action == null ? storagePref.get(activity, "pref_default_fragment", "e_journal") : action;
                log.v(TAG, "Section = ", act, " from ", (action == null ? "preference" : "intent's extras"));
                switch (act) {
                    case "e_journal":
                        selectedSection = R.id.nav_e_register;
                        break;
                    case "protocol_changes":
                        selectedSection = R.id.nav_protocol_changes;
                        break;
                    case "rating":
                        selectedSection = R.id.nav_rating;
                        break;
                    case "schedule_lessons":
                        selectedSection = R.id.nav_schedule;
                        break;
                    case "schedule_exams":
                        selectedSection = R.id.nav_schedule_exams;
                        break;
                    case "schedule_attestations":
                        selectedSection = R.id.nav_schedule_attestations;
                        break;
                    case "room101":
                        selectedSection = R.id.nav_room101;
                        break;
                    case "university":
                        selectedSection = R.id.nav_university;
                        break;
                    case "groups":
                        selectedSection = R.id.nav_group;
                        break;
                    case "scholarship":
                        selectedSection = R.id.nav_scholarship;
                        break;
                    default:
                        log.w(TAG, "unsupported act: '", act, "'. Going to select 'e_journal' instead");
                        selectedSection = R.id.nav_e_register;
                        break;
                }
                if (action != null) activity.getIntent().removeExtra("action");
            }
            // activity ready to be loaded
            loaded = false;
        });
    }

    private void reconnect() {
        authorize(LoginActivity.SIGNAL_RECONNECT);
    }

    private void toggleDrawer() {
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        if (drawerLayout == null) {
            return;
        }
        int drawerLockMode = drawerLayout.getDrawerLockMode(GravityCompat.START);
        if (drawerLayout.isDrawerVisible(GravityCompat.START)
                && (drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_OPEN)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private int applySectionUnAuthorizedMode(int section) {
        if (App.UNAUTHORIZED_MODE) {
            switch (section) {
                case R.id.nav_e_register:
                case R.id.nav_protocol_changes: section = R.id.nav_schedule; break;
                case R.id.nav_room101: section = R.id.nav_university; break;
                case R.id.nav_do_clean_auth:
                case R.id.nav_logout: section = R.id.nav_change_account; break;
            }
        }
        return section;
    }
}
