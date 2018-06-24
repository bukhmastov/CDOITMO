package com.bukhmastov.cdoitmo.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.fragment.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragment.HomeScreenInteractionFragment;
import com.bukhmastov.cdoitmo.fragment.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragment.RatingFragment;
import com.bukhmastov.cdoitmo.fragment.Room101Fragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Migration;
import com.bukhmastov.cdoitmo.util.StorageProvider;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

public class MainActivity extends ConnectedActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_SELECTION = "selectedSection";
    private static boolean initialized = false;
    public static boolean loaded = false;
    public static boolean exitOfflineMode = false;
    public static int selectedSection = -1;
    public static MenuItem selectedMenuItem = null;

    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StorageProvider storageProvider = StorageProvider.instance();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(TAG, "Activity created");
        if (!initialized) {
            // initialize app
            super.onCreate(savedInstanceState);
            Thread.run(() -> {
                try {
                    try {
                        Log.i(TAG, "App | launched");
                        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        Log.i(TAG, "App | version code = ", pInfo.versionCode);
                        Log.i(TAG, "App | sdk = ", Build.VERSION.SDK_INT);
                        Log.i(TAG, "App | theme = ", Theme.getAppTheme(activity));
                    } catch (Exception e) {
                        Log.exception(e);
                    }
                    Theme.applyActivityTheme(activity);
                    // apply compatibility changes
                    Migration.migrate(activity, storageProvider);
                    // set default preferences
                    SettingsFragment.applyDefaultValues(activity);
                    // enable/disable firebase
                    FirebaseCrashlyticsProvider.setEnabled(activity);
                    FirebaseAnalyticsProvider.setEnabled(activity);
                    FirebasePerformanceProvider.setEnabled(activity);
                    // set auto_logout value
                    LoginActivity.auto_logout = storagePref.get(activity, "pref_auto_logout", false);
                    // set first_launch and intro values
                    App.isFirstLaunchEver = storagePref.get(activity, "pref_first_launch", true);
                    App.showIntroducingActivity = App.isFirstLaunchEver;
                    if (App.isFirstLaunchEver) {
                        storagePref.put(activity, "pref_first_launch", false);
                    }
                    // firebase events and properties
                    FirebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.APP_OPEN);
                    FirebaseAnalyticsProvider.setUserProperty(activity, FirebaseAnalyticsProvider.Property.THEME, Theme.getAppTheme(activity));
                } catch (Exception e) {
                    Log.exception(e);
                } finally {
                    Log.i(TAG, "App | initialized");
                    Thread.runOnUI(() -> {
                        // app initialization completed, going to recreate activity
                        initialized = true;
                        recreate();
                    });
                }
            });
        } else {
            // regular app's runtime
            Theme.applyActivityTheme(activity);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            // setup toolbar and drawer layout
            final Toolbar toolbar = findViewById(R.id.toolbar_main);
            final DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
            if (toolbar != null) {
                Theme.applyToolbarTheme(activity, toolbar);
                setSupportActionBar(toolbar);
            }
            if (drawer_layout != null) {
                App.tablet = false;
                if (toolbar != null) {
                    ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                    drawer_layout.addDrawerListener(mDrawerToggle);
                    mDrawerToggle.syncState();
                    if (App.isFirstLaunchEver) {
                        drawer_layout.openDrawer(Gravity.START);
                    }
                }
            } else {
                App.tablet = true;
            }
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
            // track to firebase
            FirebaseAnalyticsProvider.logCurrentScreen(this);
            FirebaseAnalyticsProvider.setUserProperty(this, FirebaseAnalyticsProvider.Property.DEVICE, App.tablet ? "tablet" : "mobile");
            // setup static variables
            App.OFFLINE_MODE = "offline".equals(getIntent().getStringExtra("mode")) ||
                    !Client.isOnline(this) ||
                    (App.firstLaunch && storagePref.get(this, "pref_initial_offline", false));
            App.firstLaunch = false;
            App.isFirstLaunchEver = false;
            // do some logging
            Log.i(TAG, "Device = ", (App.tablet ? "tablet" : "mobile"));
            Log.i(TAG, "Mode = ", (App.OFFLINE_MODE ? "offline" : "online"));
            // define section to be opened
            final String action = getIntent().getStringExtra("action");
            if (action == null && savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_SELECTION)) {
                selectedSection = savedInstanceState.getInt(STATE_SELECTED_SELECTION);
                Log.v(TAG, "Section selected from savedInstanceState");
            } else {
                String act = action == null ? storagePref.get(this, "pref_default_fragment", "e_journal") : action;
                Log.v(TAG, "Section = ", act, " from ", (action == null ? "preference" : "intent's extras"));
                switch (act) {
                    case "e_journal": selectedSection = R.id.nav_e_register; break;
                    case "protocol_changes": selectedSection = R.id.nav_protocol_changes; break;
                    case "rating": selectedSection = R.id.nav_rating; break;
                    case "schedule_lessons": selectedSection = R.id.nav_schedule; break;
                    case "schedule_exams": selectedSection = R.id.nav_schedule_exams; break;
                    case "schedule_attestations": selectedSection = R.id.nav_schedule_attestations; break;
                    case "room101": selectedSection = R.id.nav_room101; break;
                    case "university": selectedSection = R.id.nav_university; break;
                    default:
                        Log.wtf(TAG, "unsupported act: '", act, "'. Going to select 'e_journal' instead");
                        selectedSection = R.id.nav_e_register;
                        break;
                }
                if (action != null) getIntent().removeExtra("action");
            }
            // activity ready to be loaded
            loaded = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Activity resumed");
        if (initialized) {
            final NavigationView navigationView = activity.findViewById(R.id.nav_view);
            NavigationMenu.displayEnableDisableOfflineButton(navigationView);
            NavigationMenu.hideIfUnauthorizedMode(navigationView);
            if (!exitOfflineMode && (App.OFFLINE_MODE || Account.authorized)) {
                authorized();
            } else {
                authorize(LoginActivity.SIGNAL_LOGIN);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "Activity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
        loaded = false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        Log.v(TAG, "NavigationItemSelected | item=", item.getTitle());
        DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
        if (drawer_layout != null) drawer_layout.closeDrawer(GravityCompat.START);
        selectSection(item.getItemId());
        return true;
    }

    @Override
    public void onBackPressed() {
        try {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
            if (back()) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        toolbar = menu;
        BottomBar.snackBarOffline(this);
        NavigationMenu.toggleOfflineIcon(toolbar);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode: authorize(LoginActivity.SIGNAL_RECONNECT); return true;
            default: return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (initialized && selectedSection != -1) {
            savedInstanceState.putInt(STATE_SELECTED_SELECTION, selectedSection);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected int getRootViewId() {
        return R.id.activity_main;
    }

    private void authorize(final int state) {
        Thread.run(() -> {
            try {
                Log.v(TAG, "authorize | state=", state);
                loaded = false;
                exitOfflineMode = false;
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.putExtra("state", state);
                activity.startActivity(intent);
            } catch (Exception ignore) {/* ignore */}
        });
    }
    private void authorized() {
        Thread.run(() -> {
            Log.v(TAG, "authorized");
            if (!loaded) {
                loaded = true;
                Thread.run(Thread.BACKGROUND, () -> new ProtocolTracker(activity).check());
                selectSection(selectedSection);
                NavigationMenu.displayUserData(activity, storage, findViewById(R.id.nav_view));
                NavigationMenu.displayRemoteMessage(activity, storage);
                BottomBar.snackBarOffline(activity);
                NavigationMenu.toggleOfflineIcon(toolbar);
            } else if (selectedMenuItem != null) {
                try {
                    selectSection(selectedMenuItem.getItemId());
                } finally {
                    selectedMenuItem = null;
                }
            }
        });
    }
    private void selectSection(final int s) {
        final int section;
        if (App.UNAUTHORIZED_MODE) {
            switch (s) {
                case R.id.nav_e_register:
                case R.id.nav_protocol_changes: section = R.id.nav_schedule; break;
                case R.id.nav_room101: section = R.id.nav_university; break;
                case R.id.nav_do_clean_auth:
                case R.id.nav_logout: section = R.id.nav_change_account; break;
                default: section = s; break;
            }
        } else {
            section = s;
        }
        Thread.run(() -> {
            Log.v(TAG, "selectSection | section=", section);
            switch (section) {
                case R.id.nav_e_register:
                case R.id.nav_protocol_changes:
                case R.id.nav_rating:
                case R.id.nav_schedule:
                case R.id.nav_schedule_exams:
                case R.id.nav_schedule_attestations:
                case R.id.nav_room101:
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
                        case R.id.nav_university: connectedFragmentClass = UniversityFragment.class; break;
                    }
                    Thread.runOnUI(() -> {
                        if (openFragment(TYPE.ROOT, connectedFragmentClass, null)) {
                            ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(section);
                            selectedSection = section;
                        } else {
                            BottomBar.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), view -> selectSection(section));
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
                    Thread.runOnUI(() -> {
                        if (openActivityOrFragment(TYPE.ROOT, connectedFragmentClass, null)) {
                            if (App.tablet) {
                                ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(section);
                                selectedSection = section;
                            }
                        } else {
                            BottomBar.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), view -> selectSection(section));
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
                            break;
                    }
                    switch (section) {
                        case R.id.nav_enable_offline_mode: authorize(LoginActivity.SIGNAL_GO_OFFLINE); break;
                        case R.id.nav_disable_offline_mode: authorize(LoginActivity.SIGNAL_RECONNECT); break;
                        case R.id.nav_change_account: authorize(LoginActivity.SIGNAL_CHANGE_ACCOUNT); break;
                        case R.id.nav_do_clean_auth: authorize(LoginActivity.SIGNAL_DO_CLEAN_AUTH); break;
                        case R.id.nav_logout: Account.logoutConfirmation(activity, () -> authorize(LoginActivity.SIGNAL_LOGOUT)); break;
                    }
                    break;
                }
            }
        });
    }
}
