package com.bukhmastov.cdoitmo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragments.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragments.RatingFragment;
import com.bukhmastov.cdoitmo.fragments.Room101Fragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_SELECTION = "selectedSection";
    public static int selectedSection = R.id.nav_e_register;
    private NavigationView navigationView;
    public static Menu menu;
    public static boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, ((Toolbar)findViewById(R.id.toolbar_main)), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
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
                default: selectedSection = R.id.nav_e_register; break;
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
        Log.v(TAG, "BackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        MainActivity.menu = menu;
        checkOffline();
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
        Log.v(TAG, "authorize");
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
            checkOffline();
            selectSection(selectedSection);
            displayUserData();
        }
    }
    private void checkOffline(){
        Log.v(TAG, "checkOffline " + (Static.OFFLINE_MODE ? "offline" : "normal"));
        if (Static.OFFLINE_MODE) {
            Static.snackBar(this, getString(R.string.offline_mode_on));
        }
        if (MainActivity.menu != null) {
            if (Static.OFFLINE_MODE) {
                for (int i = 0; i < MainActivity.menu.size(); i++) {
                    MainActivity.menu.getItem(i).setVisible(false);
                }
            }
            MenuItem menuItem = MainActivity.menu.findItem(R.id.offline_mode);
            if (menuItem != null) {
                menuItem.setVisible(Static.OFFLINE_MODE);
            }
        }
    }
    private void displayUserData(){
        displayUserData(R.id.user_name, Storage.file.perm.get(this, "user#name"));
        displayUserData(R.id.user_group, Storage.file.perm.get(this, "user#group"));
    }
    private void displayUserData(int id, String text){
        Log.v(TAG, "displayUserData " + text);
        if (navigationView == null) return;
        View activity_main_nav_header = navigationView.getHeaderView(0);
        if (activity_main_nav_header == null) return;
        TextView textView = (TextView) activity_main_nav_header.findViewById(id);
        if (textView != null) {
            if (!text.isEmpty()) {
                textView.setText(text);
                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            }
        }
    }

    private void selectSection(final int section){
        Class fragmentClass = null;
        String title = null;
        switch(section){
            case R.id.nav_e_register:
                title = getString(R.string.e_journal);
                fragmentClass = ERegisterFragment.class;
                break;
            case R.id.nav_protocol_changes:
                title = getString(R.string.protocol_changes);
                fragmentClass = ProtocolFragment.class;
                break;
            case R.id.nav_rating:
                title = getString(R.string.rating);
                fragmentClass = RatingFragment.class;
                break;
            case R.id.nav_schedule:
                title = getString(R.string.schedule_lessons);
                fragmentClass = ScheduleLessonsFragment.class;
                break;
            case R.id.nav_schedule_exams:
                title = getString(R.string.schedule_exams);
                fragmentClass = ScheduleExamsFragment.class;
                break;
            case R.id.nav_room101:
                title = getString(R.string.room101);
                fragmentClass = Room101Fragment.class;
                break;
            case R.id.nav_shortcuts: startActivity(new Intent(this, ShortcutCreateActivity.class)); break;
            case R.id.nav_settings: startActivity(new Intent(this, SettingsActivity.class)); break;
            case R.id.nav_change_account:
                loaded = false;
                authorize(LoginActivity.SIGNAL_CHANGE_ACCOUNT);
                break;
            case R.id.nav_logout:
                loaded = false;
                authorize(LoginActivity.SIGNAL_LOGOUT);
                break;
        }
        if (fragmentClass != null) {
            Log.v(TAG, "selectSection | fragmentClass != null | " + title);
            navigationView.setCheckedItem(section);
            selectedSection = section;
            ViewGroup content_container = (ViewGroup) findViewById(R.id.content_container);
            if (content_container != null) content_container.removeAllViews();
            try {
                Fragment fragment = (Fragment) fragmentClass.newInstance();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_container, fragment).commit();
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
            } catch (Exception e) {
                Static.error(e);
                if (content_container != null) {
                    Snackbar snackbar = Snackbar.make(content_container, getString(R.string.failed_to_open_fragment), Snackbar.LENGTH_SHORT);
                    snackbar.getView().setBackgroundColor(Static.colorBackgroundSnackBar);
                    snackbar.setAction(R.string.redo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectSection(section);
                        }
                    });
                    snackbar.show();
                } else {
                    Log.w(TAG, "selectSection | content_container is null");
                }
            }
        }
    }

}