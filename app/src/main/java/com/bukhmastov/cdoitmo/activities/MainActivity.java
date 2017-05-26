package com.bukhmastov.cdoitmo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragments.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragments.RatingFragment;
import com.bukhmastov.cdoitmo.fragments.Room101Fragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragments.ShortcutCreateFragment;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class MainActivity extends ConnectedActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_SELECTION = "selectedSection";
    public static int selectedSection = R.id.nav_e_register;
    public static Menu menu;
    public static boolean loaded = false;
    public static MenuItem selectedMenuItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            Static.tablet = false;
            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, ((Toolbar) findViewById(R.id.toolbar_main)), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            mDrawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        } else {
            Static.tablet = true;
        }
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Static.OFFLINE_MODE = !Static.isOnline(this) || (Static.firstLaunch && Storage.pref.get(this, "pref_initial_offline", false));
        Static.init(this);
        Static.firstLaunch = false;

        Log.i(TAG, "mode=" + (Static.OFFLINE_MODE ? "offline" : "normal"));

        String action = getIntent().getStringExtra("action");
        if (savedInstanceState == null || action != null) {
            String act = action == null ? Storage.pref.get(this, "pref_default_fragment", "e_journal") : action;
            Log.v(TAG, "Section = " + act + " from " + (action == null ? "preference" : "intent's extras"));
            switch (act) {
                case "e_journal": selectedSection = R.id.nav_e_register; break;
                case "protocol_changes": selectedSection = R.id.nav_protocol_changes; break;
                case "rating": selectedSection = R.id.nav_rating; break;
                case "schedule_lessons": selectedSection = R.id.nav_schedule; break;
                case "schedule_exams": selectedSection = R.id.nav_schedule_exams; break;
                case "room101": selectedSection = R.id.nav_room101; break;
                default:
                    Log.wtf(TAG, "unsupported act: '" + act + "'. Going to select 'e_journal' instead");
                    selectedSection = R.id.nav_e_register;
                    break;
            }
            if (action != null) getIntent().removeExtra("action");
        } else {
            selectedSection = savedInstanceState.getInt(STATE_SELECTED_SELECTION);
        }

        loaded = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Activity resumed");
        Static.NavigationMenu.displayEnableDisableOfflineButton((NavigationView) findViewById(R.id.nav_view));
        if (selectedMenuItem != null) {
            try {
                selectSection(selectedMenuItem.getItemId());
            } finally {
                selectedMenuItem = null;
            }
        }
        if (Static.OFFLINE_MODE) {
            authorized();
        } else {
            if (Static.authorized) {
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.v(TAG, "NavigationItemSelected " + item.getTitle());
        DrawerLayout drawer_layout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer_layout != null) drawer_layout.closeDrawer(GravityCompat.START);
        selectSection(item.getItemId());
        return true;
    }

    @Override
    public void onBackPressed() {
        try {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        MainActivity.menu = menu;
        Static.NavigationMenu.snackbarOffline(this);
        Static.NavigationMenu.drawOffline(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode:
                authorize(LoginActivity.SIGNAL_RECONNECT);
                return true;
            default: return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_SELECTED_SELECTION, selectedSection);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void authorize(int state){
        Log.v(TAG, "authorize | state=" + state);
        loaded = false;
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("state", state);
        startActivity(intent);
    }
    private void authorized(){
        Log.v(TAG, "authorized");
        if (!loaded) {
            loaded = true;
            if (Static.protocolTracker == null) Static.protocolTracker = new ProtocolTracker(this);
            Static.protocolTracker.check();
            selectSection(selectedSection);
            Static.NavigationMenu.displayUserData(this, (NavigationView) findViewById(R.id.nav_view));
            Static.NavigationMenu.displayUserAvatar(this, (NavigationView) findViewById(R.id.nav_view));
            Static.NavigationMenu.snackbarOffline(this);
            Static.NavigationMenu.drawOffline(menu);
        }
    }

    private void selectSection(final int section){
        switch (section) {
            case R.id.nav_e_register:
            case R.id.nav_protocol_changes:
            case R.id.nav_rating:
            case R.id.nav_schedule:
            case R.id.nav_schedule_exams:
            case R.id.nav_room101: {
                Class connectedFragmentClass;
                switch (section) {
                    default:
                    case R.id.nav_e_register: connectedFragmentClass = ERegisterFragment.class; break;
                    case R.id.nav_protocol_changes: connectedFragmentClass = ProtocolFragment.class; break;
                    case R.id.nav_rating: connectedFragmentClass = RatingFragment.class; break;
                    case R.id.nav_schedule: connectedFragmentClass = ScheduleLessonsFragment.class; break;
                    case R.id.nav_schedule_exams: connectedFragmentClass = ScheduleExamsFragment.class; break;
                    case R.id.nav_room101: connectedFragmentClass = Room101Fragment.class; break;
                }
                if (openFragment(TYPE.root, connectedFragmentClass, null)) {
                    ((NavigationView) findViewById(R.id.nav_view)).setCheckedItem(section);
                    selectedSection = section;
                } else {
                    Static.snackBar(this, getString(R.string.failed_to_open_fragment), getString(R.string.redo), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectSection(section);
                        }
                    });
                }
                break;
            }
            case R.id.nav_shortcuts:
                if (openActivityOrFragment(TYPE.stackable, ShortcutCreateFragment.class, null)) {
                    if (Static.tablet) {
                        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
                        for (int i = 0; i < menu.size(); i++) {
                            menu.getItem(i).setChecked(false);
                        }
                    }
                }
                break;
            case R.id.nav_settings: startActivity(new Intent(this, SettingsActivity.class)); break;
            case R.id.nav_enable_offline_mode:
            case R.id.nav_disable_offline_mode:
            case R.id.nav_change_account:
            case R.id.nav_logout: {
                loaded = false;
                switch (section) {
                    case R.id.nav_enable_offline_mode: authorize(LoginActivity.SIGNAL_GO_OFFLINE); break;
                    case R.id.nav_disable_offline_mode: authorize(LoginActivity.SIGNAL_RECONNECT); break;
                    case R.id.nav_change_account: authorize(LoginActivity.SIGNAL_CHANGE_ACCOUNT); break;
                    case R.id.nav_logout: authorize(LoginActivity.SIGNAL_LOGOUT); break;
                }
                break;
            }
        }
    }

    @Override
    protected int getRootViewId() {
        return R.id.activity_main;
    }

}