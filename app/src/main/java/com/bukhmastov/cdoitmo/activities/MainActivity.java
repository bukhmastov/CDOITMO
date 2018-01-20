package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
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
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragments.HomeScreenInteractionFragment;
import com.bukhmastov.cdoitmo.fragments.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragments.RatingFragment;
import com.bukhmastov.cdoitmo.fragments.Room101Fragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.utils.Account;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.bukhmastov.cdoitmo.utils.Wipe;

public class MainActivity extends ConnectedActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private final Activity activity = this;
    private static final String STATE_SELECTED_SELECTION = "selectedSection";
    private static boolean initialized = false;
    public static boolean loaded = false;
    public static int selectedSection = -1;
    public static MenuItem selectedMenuItem = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(TAG, "Activity created");
        if (!initialized) {
            // initialize app
            super.onCreate(savedInstanceState);
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        try {
                            Log.i(TAG, "App | launched");
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            Log.i(TAG, "App | version code = " + pInfo.versionCode);
                            Log.i(TAG, "App | sdk = " + Build.VERSION.SDK_INT);
                            Log.i(TAG, "App | theme = " + Static.getAppTheme(activity));
                        } catch (Exception e) {
                            Static.error(e);
                        }
                        Static.applyActivityTheme(activity);
                        // apply compatibility changes
                        Wipe.check(activity);
                        // set default preferences
                        SettingsFragment.applyDefaultValues(activity);
                        // enable/disable firebase
                        FirebaseCrashlyticsProvider.setEnabled(activity);
                        FirebaseAnalyticsProvider.setEnabled(activity);
                        // init static variables
                        Static.init(activity);
                        // set auto_logout value
                        LoginActivity.auto_logout = Storage.pref.get(activity, "pref_auto_logout", false);
                        // set first_launch and intro values
                        Static.isFirstLaunchEver = Storage.pref.get(activity, "pref_first_launch", true);
                        Static.showIntroducingActivity = Static.isFirstLaunchEver;
                        if (Static.isFirstLaunchEver) {
                            Storage.pref.put(activity, "pref_first_launch", false);
                        }
                        // firebase events and properties
                        FirebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.APP_OPEN);
                        FirebaseAnalyticsProvider.setUserProperty(activity, FirebaseAnalyticsProvider.Property.THEME, Static.getAppTheme(activity));
                    } catch (Exception e) {
                        Static.error(e);
                    } finally {
                        Log.i(TAG, "App | initialized");
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // app initialization completed, going to recreate activity
                                initialized = true;
                                recreate();
                            }
                        });
                    }
                }
            });
        } else {
            // regular app's runtime
            Static.applyActivityTheme(activity);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            // setup toolbar and drawer layout
            final Toolbar toolbar = findViewById(R.id.toolbar_main);
            final DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
            if (toolbar != null) {
                Static.applyToolbarTheme(activity, toolbar);
                setSupportActionBar(toolbar);
            }
            if (drawer_layout != null) {
                Static.tablet = false;
                if (toolbar != null) {
                    ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                    drawer_layout.addDrawerListener(mDrawerToggle);
                    mDrawerToggle.syncState();
                    if (Static.isFirstLaunchEver) {
                        drawer_layout.openDrawer(Gravity.START);
                    }
                }
            } else {
                Static.tablet = true;
            }
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
            // track to firebase
            FirebaseAnalyticsProvider.logCurrentScreen(this);
            FirebaseAnalyticsProvider.setUserProperty(this, FirebaseAnalyticsProvider.Property.DEVICE, Static.tablet ? "tablet" : "mobile");
            // setup static variables
            Static.OFFLINE_MODE = !Static.isOnline(this) || (Static.firstLaunch && Storage.pref.get(this, "pref_initial_offline", false));
            Static.firstLaunch = false;
            Static.isFirstLaunchEver = false;
            // do some logging
            Log.i(TAG, "Device = " + (Static.tablet ? "tablet" : "mobile"));
            Log.i(TAG, "Mode = " + (Static.OFFLINE_MODE ? "offline" : "online"));
            // define section to be opened
            final String action = getIntent().getStringExtra("action");
            if (action == null && savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_SELECTION)) {
                selectedSection = savedInstanceState.getInt(STATE_SELECTED_SELECTION);
                Log.v(TAG, "Section selected from savedInstanceState");
            } else {
                String act = action == null ? Storage.pref.get(this, "pref_default_fragment", "e_journal") : action;
                Log.v(TAG, "Section = " + act + " from " + (action == null ? "preference" : "intent's extras"));
                switch (act) {
                    case "e_journal": selectedSection = R.id.nav_e_register; break;
                    case "protocol_changes": selectedSection = R.id.nav_protocol_changes; break;
                    case "rating": selectedSection = R.id.nav_rating; break;
                    case "schedule_lessons": selectedSection = R.id.nav_schedule; break;
                    case "schedule_exams": selectedSection = R.id.nav_schedule_exams; break;
                    case "room101": selectedSection = R.id.nav_room101; break;
                    case "university": selectedSection = R.id.nav_university; break;
                    default:
                        Log.wtf(TAG, "unsupported act: '" + act + "'. Going to select 'e_journal' instead");
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
            Static.NavigationMenu.displayEnableDisableOfflineButton((NavigationView) findViewById(R.id.nav_view));
            if (Static.OFFLINE_MODE || Account.authorized) {
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
        Log.v(TAG, "NavigationItemSelected " + item.getTitle());
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
        Static.NavigationMenu.snackbarOffline(this);
        Static.NavigationMenu.drawOffline(toolbar);
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

    private void authorize(final int state) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "authorize | state=" + state);
                loaded = false;
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.putExtra("state", state);
                startActivity(intent);
            }
        });
    }
    private void authorized() {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "authorized");
                if (!loaded) {
                    loaded = true;
                    Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                        @Override
                        public void run() {
                            new ProtocolTracker(activity).check();
                        }
                    });
                    selectSection(selectedSection);
                    Static.NavigationMenu.displayUserData(activity, (NavigationView) findViewById(R.id.nav_view));
                    Static.NavigationMenu.displayRemoteMessage(activity);
                    Static.NavigationMenu.snackbarOffline(activity);
                    Static.NavigationMenu.drawOffline(toolbar);
                } else if (selectedMenuItem != null) {
                    try {
                        selectSection(selectedMenuItem.getItemId());
                    } finally {
                        selectedMenuItem = null;
                    }
                }
            }
        });
    }
    private void selectSection(final int section) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "selectSection | section=" + section);
                switch (section) {
                    case R.id.nav_e_register:
                    case R.id.nav_protocol_changes:
                    case R.id.nav_rating:
                    case R.id.nav_schedule:
                    case R.id.nav_schedule_exams:
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
                            case R.id.nav_room101: connectedFragmentClass = Room101Fragment.class; break;
                            case R.id.nav_university: connectedFragmentClass = UniversityFragment.class; break;
                        }
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (openFragment(TYPE.root, connectedFragmentClass, null)) {
                                    ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(section);
                                    selectedSection = section;
                                } else {
                                    Static.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            selectSection(section);
                                        }
                                    });
                                }
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
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (openActivityOrFragment(TYPE.root, connectedFragmentClass, null)) {
                                    if (Static.tablet) {
                                        ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(section);
                                        selectedSection = section;
                                    }
                                } else {
                                    Static.snackBar(activity, activity.getString(R.string.failed_to_open_fragment), activity.getString(R.string.redo), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            selectSection(section);
                                        }
                                    });
                                }
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
                            case R.id.nav_enable_offline_mode: authorize(LoginActivity.SIGNAL_GO_OFFLINE); break;
                            case R.id.nav_disable_offline_mode: authorize(LoginActivity.SIGNAL_RECONNECT); break;
                            case R.id.nav_change_account: authorize(LoginActivity.SIGNAL_CHANGE_ACCOUNT); break;
                            case R.id.nav_do_clean_auth: authorize(LoginActivity.SIGNAL_DO_CLEAN_AUTH); break;
                            case R.id.nav_logout: Account.logoutConfirmation(activity, new Static.SimpleCallback() {
                                @Override
                                public void onCall() {
                                    authorize(LoginActivity.SIGNAL_LOGOUT);
                                }
                            }); break;
                        }
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected int getRootViewId() {
        return R.id.activity_main;
    }
}
