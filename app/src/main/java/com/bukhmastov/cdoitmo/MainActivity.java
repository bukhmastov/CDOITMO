package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    public static int selectedSection = R.id.nav_e_register;
    public static SharedPreferences sharedPreferences;
    public static ProtocolTracker protocolTracker;
    public static String group = null;
    public static String name = null;
    public static int week = -1;
    private NavigationView navigationView;
    private boolean loaded = false;
    private RequestHandle checkRequestHandle = null;
    static boolean OFFLINE_MODE = false;
    static Menu menu;
    static TypedValue typedValue;
    static int textColorPrimary;
    static int textColorSecondary;
    static int colorSeparator;
    static float destiny;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false)) setTheme(R.style.AppTheme_Dark);
        OFFLINE_MODE = !DeIfmoRestClient.isOnline();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, ((Toolbar)findViewById(R.id.toolbar_main)), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        switch(sharedPreferences.getString("pref_default_fragment", "e_journal")){
            case "e_journal": selectedSection = R.id.nav_e_register; break;
            case "protocol_changes": selectedSection = R.id.nav_protocol_changes; break;
            case "rating": selectedSection = R.id.nav_rating; break;
            case "schedule_lessons": selectedSection = R.id.nav_schedule; break;
            case "schedule_exams": selectedSection = R.id.nav_schedule_exams; break;
            case "room101": selectedSection = R.id.nav_room101; break;
        }
        protocolTracker = new ProtocolTracker(this);
        typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textColorPrimary = obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary}).getColor(0, -1);
        getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        textColorSecondary = obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorSecondary}).getColor(0, -1);
        getTheme().resolveAttribute(R.attr.colorSeparator, typedValue, true);
        colorSeparator = obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorSeparator}).getColor(0, -1);
        destiny = getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(checkRequestHandle != null) checkRequestHandle.cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OFFLINE_MODE){
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Приложение запущено в оффлайн режиме", Snackbar.LENGTH_LONG);
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, typedValue, true);
            snackbar.getView().setBackgroundColor(typedValue.data);
            snackbar.show();
        }
        updateWeek();
        navigationView.setCheckedItem(selectedSection);
        if(!loaded) check();
    }

    @Override
    protected void onDestroy() {
        if(sharedPreferences.getBoolean("pref_auto_logout", false)) gotoLogin(LoginActivity.SIGNAL_LOGOUT);
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        selectSection(item.getItemId());
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(sharedPreferences.getBoolean("pref_auto_logout", false) || OFFLINE_MODE){
                super.onBackPressed();
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        MainActivity.menu = menu;
        if(OFFLINE_MODE) menu.findItem(R.id.offline_mode).setVisible(true);
        // search view for lessons schedule
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSubmitButtonEnabled(true);
        searchView.setElevation(6);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    MainActivity.menu.findItem(R.id.action_search).collapseActionView();
                    if(selectedSection == R.id.nav_schedule) {
                        if (ScheduleLessonsFragment.scheduleLessons != null) ScheduleLessonsFragment.scheduleLessons.search(query, false);
                    }
                    if(selectedSection == R.id.nav_schedule_exams) {
                        if (ScheduleExamsFragment.scheduleExams != null) ScheduleExamsFragment.scheduleExams.search(query, false);
                    }
                } catch (Exception e){
                    LoginActivity.errorTracker.add(e);
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode:
                LoginActivity.state = LoginActivity.SIGNAL_RECONNECT;
                finish();
                return true;
            default: return false;
        }
    }

    private void check(){
        loaded = false;
        if(!OFFLINE_MODE) {
            draw(R.layout.state_loading);
            DeIfmoRestClient.check(new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    MainActivity.group = Storage.get(getBaseContext(), "group");
                    MainActivity.name = Storage.get(getBaseContext(), "name");
                    updateWeek();
                    if (!Objects.equals(MainActivity.name, "")) {
                        TextView user_name = (TextView) findViewById(R.id.user_name);
                        user_name.setText(response);
                        user_name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    protocolTracker.check();
                    loaded = true;
                    selectSection(selectedSection);
                }
                @Override
                public void onProgress(int state) {
                    draw(R.layout.state_loading);
                    TextView loading_message = (TextView) findViewById(R.id.loading_message);
                    switch (state) {
                        case DeIfmoRestClient.STATE_CHECKING:
                            loading_message.setText(R.string.auth_check);
                            break;
                        case DeIfmoRestClient.STATE_AUTHORIZATION:
                            loading_message.setText(R.string.authorization);
                            break;
                        case DeIfmoRestClient.STATE_AUTHORIZED:
                            loading_message.setText(R.string.authorized);
                            break;
                    }
                }
                @Override
                public void onFailure(int state) {
                    switch (state) {
                        case DeIfmoRestClient.FAILED_OFFLINE:
                            draw(R.layout.state_offline);
                            findViewById(R.id.offline_reload).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    check();
                                }
                            });
                            break;
                        case DeIfmoRestClient.FAILED_TRY_AGAIN:
                        case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                            draw(R.layout.state_try_again);
                            if (state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN)
                                ((TextView) findViewById(R.id.try_again_message)).setText(R.string.auth_failed);
                            findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    check();
                                }
                            });
                            break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                            gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                            break;
                        case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                            gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                            break;
                    }
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    checkRequestHandle = requestHandle;
                }
            });
        } else {
            MainActivity.group = Storage.get(getBaseContext(), "group");
            MainActivity.name = Storage.get(getBaseContext(), "name");
            if(!Objects.equals(name, "")){
                TextView user_name = (TextView) findViewById(R.id.user_name);
                if(user_name != null) {
                    user_name.setText(name);
                    user_name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
            protocolTracker.check();
            loaded = true;
            selectSection(selectedSection);
        }
    }
    private void updateWeek(){
        try {
            String weekStr = Storage.get(getBaseContext(), "week");
            if(!Objects.equals(weekStr, "")){
                JSONObject jsonObject = new JSONObject(weekStr);
                int week = jsonObject.getInt("week");
                if(week >= 0){
                    Calendar past = Calendar.getInstance();
                    past.setTimeInMillis(jsonObject.getLong("timestamp"));
                    MainActivity.week = week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                }
            }
        } catch (JSONException e) {
            LoginActivity.errorTracker.add(e);
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
            case R.id.nav_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout: gotoLogin(LoginActivity.SIGNAL_LOGOUT); break;
        }
        if(fragmentClass != null){
            if(!loaded){
                Snackbar snackbar = Snackbar.make(findViewById(R.id.content_container), R.string.w8m8, Snackbar.LENGTH_SHORT);
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, typedValue, true);
                snackbar.getView().setBackgroundColor(typedValue.data);
                snackbar.show();
                navigationView.setCheckedItem(selectedSection);
                return;
            }
            navigationView.setCheckedItem(section);
            ((ViewGroup) findViewById(R.id.content_container)).removeAllViews();
            selectedSection = section;
            try {
                Fragment fragment = (Fragment) fragmentClass.newInstance();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_container, fragment).commit();
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
            } catch (Exception e) {
                LoginActivity.errorTracker.add(e);
                Snackbar snackbar = Snackbar.make(findViewById(R.id.content_container), getString(R.string.failed_to_open_fragment), Snackbar.LENGTH_SHORT);
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, typedValue, true);
                snackbar.getView().setBackgroundColor(typedValue.data);
                snackbar.setAction(R.string.redo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectSection(section);
                    }
                });
                snackbar.show();
            }
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        finish();
    }
    private void draw(int layoutId){
        try {
            ViewGroup vg = ((ViewGroup) findViewById(R.id.content_container));
            vg.removeAllViews();
            vg.addView(((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Exception e) {
            LoginActivity.errorTracker.add(e);
        }
    }
}

class Cache {
    static private final String KEY_PREFIX = "cache_";
    static boolean enabled = true;
    static String get(Context context, String key){
        check(context);
        if(enabled){
            return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PREFIX + key, "");
        } else {
            return "";
        }
    }
    static void put(Context context, String key, String value){
        check(context);
        if(enabled) PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PREFIX + key, value).apply();
    }
    static void check(Context context){
        enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_use_cache", true);
        if(!enabled) clear(context);
    }
    static void clear(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> list = sharedPreferences.getAll();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for(Map.Entry<String, ?> entry : list.entrySet()) {
            if(Pattern.compile("^"+KEY_PREFIX+".*").matcher(entry.getKey()).find()) editor.remove(entry.getKey());
        }
        editor.apply();
    }
}
class Storage {
    static private final String KEY_PREFIX = "storage_";
    static String get(Context context, String key){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PREFIX + key, "");
    }
    static void put(Context context, String key, String value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PREFIX + key, value).apply();
    }
    static void delete(Context context, String key){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences.contains(key)) sharedPreferences.edit().remove(key).apply();
    }
    static void clear(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> list = sharedPreferences.getAll();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for(Map.Entry<String, ?> entry : list.entrySet()) {
            if(Pattern.compile("^"+KEY_PREFIX+".*").matcher(entry.getKey()).find()) editor.remove(entry.getKey());
        }
        editor.apply();
    }
}