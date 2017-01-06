package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    public static int selectedSection = R.id.nav_e_register;
    public static SharedPreferences sharedPreferences;
    public static ProtocolTracker protocolTracker;
    public static String group = null;
    public static String name = null;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        }
        protocolTracker = new ProtocolTracker(this);
        protocolTracker.check();
        check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setCheckedItem(selectedSection);
    }

    @Override
    protected void onDestroy() {
        if(sharedPreferences.getBoolean("pref_auto_logout", false)) gotoLogin(LoginActivity.SIGNAL_LOGOUT);
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START); // закрываем меню
        selectSection(item.getItemId()); // выполняем выбор секции меню
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void check(){
        draw(R.layout.state_loading);
        DeIfmoRestClient.check(new DeIfmoRestClientResponseHandler() {
            @Override
            public void onSuccess(int statusCode, String response) {
                if(!response.isEmpty()) {
                    TextView user_name = (TextView) findViewById(R.id.user_name);
                    user_name.setText(response);
                    user_name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                selectSection(selectedSection);
            }
            @Override
            public void onProgress(int state) {
                draw(R.layout.state_loading);
                TextView loading_message = (TextView) findViewById(R.id.loading_message);
                switch(state){
                    case DeIfmoRestClient.STATE_CHECKING: loading_message.setText(R.string.auth_check); break;
                    case DeIfmoRestClient.STATE_AUTHORIZATION: loading_message.setText(R.string.authorization); break;
                    case DeIfmoRestClient.STATE_AUTHORIZED: loading_message.setText(R.string.authorized); break;
                }
            }
            @Override
            public void onFailure(int state) {
                switch(state){
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
                        if(state == DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN) ((TextView) findViewById(R.id.try_again_message)).setText(R.string.auth_failed);
                        findViewById(R.id.try_again_reload).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                check();
                            }
                        });
                        break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED); break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED: gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED); break;
                }
            }
        });
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
            case R.id.nav_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout: gotoLogin(LoginActivity.SIGNAL_LOGOUT); break;
        }
        if(fragmentClass != null){
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
                e.printStackTrace();
                Snackbar.make(findViewById(R.id.content_container), R.string.failed_to_open_fragment, Snackbar.LENGTH_LONG).setAction(R.string.redo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectSection(section);
                    }
                }).show();
            }
        }
    }
    void gotoLogin(int state){
        LoginActivity.state = state;
        finish();
    }
    private void draw(int layoutId){
        ViewGroup vg = ((ViewGroup) findViewById(R.id.content_container));
        vg.removeAllViews();
        vg.addView(((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}

class Cache {
    static boolean enabled = true;
    static String get(Context context, String key){
        check(context);
        if(enabled){
            return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
        } else {
            return "";
        }
    }
    static void put(Context context, String key, String value){
        check(context);
        if(enabled){
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
        }
    }
    static void check(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        enabled = sharedPreferences.getBoolean("pref_use_cache", true);
        if(!enabled){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ERegister", "");
            editor.putString("Protocol", "");
            editor.putString("Rating", "");
            editor.putString("RatingList", "");
            editor.apply();
        }
    }
}