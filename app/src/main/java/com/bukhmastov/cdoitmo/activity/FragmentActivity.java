package com.bukhmastov.cdoitmo.activity;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

public class FragmentActivity extends ConnectedActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FragmentActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Theme.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        try {
            final Intent intent = getIntent();
            if (intent != null) {
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
                        layout_with_menu = fragment_extras.getBoolean(ConnectedActivity.ACTIVITY_WITH_MENU);
                        fragment_extras.remove(ConnectedActivity.ACTIVITY_WITH_MENU);
                    }
                } else {
                    throw new NullPointerException("Intent's extras cannot be null");
                }
                setContentView(layout_with_menu ? R.layout.activity_fragment : R.layout.activity_fragment_without_menu);
                final Toolbar toolbar = findViewById(R.id.toolbar_fragment);
                if (toolbar != null) {
                    Theme.applyToolbarTheme(this, toolbar);
                    setSupportActionBar(toolbar);
                }
                final ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setHomeButtonEnabled(true);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                if (layout_with_menu) {
                    ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);
                }
                invoke(fragment_class, fragment_extras);
            } else {
                throw new NullPointerException("Intent cannot be null");
            }
        } catch (Exception e) {
            Log.exception(e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layout_with_menu) {
            NavigationView navigationView = findViewById(R.id.nav_view);
            NavigationMenu.displayEnableDisableOfflineButton(navigationView);
            NavigationMenu.hideIfUnauthorizedMode(navigationView);
            NavigationMenu.displayUserData(this, navigationView);
            NavigationMenu.displayRemoteMessage(this);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (layout_with_menu) {
            Log.v(TAG, "NavigationItemSelected " + item.getTitle());
            DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
            if (drawer_layout != null) {
                drawer_layout.closeDrawer(GravityCompat.START);
            }
            MainActivity.selectedMenuItem = item;
            finish();
        }
        return true;
    }

    @Override
    protected int getRootViewId() {
        return R.id.activity_fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        toolbar = menu;
        NavigationMenu.toggleOfflineIcon(toolbar);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.offline_mode: MainActivity.exitOfflineMode = true;
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (!layout_with_menu) {
                throw new Exception("");
            }
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

    public void invoke(final Class connectedFragmentClass, final Bundle extras) {
        final FragmentActivity self = this;
        final FragmentManager fragmentManager = getSupportFragmentManager();
        Thread.runOnUI(() -> {
            Log.v(TAG, "invoke | " + connectedFragmentClass.toString());
            try {
                ConnectedFragment.Data data = ConnectedFragment.getData(self, connectedFragmentClass);
                if (data == null) {
                    throw new NullPointerException("data cannot be null");
                }
                updateToolbar(self, data.title, null);
                ViewGroup root = findViewById(getRootViewId());
                if (root != null) {
                    root.removeAllViews();
                }
                Fragment fragment = (Fragment) data.connectedFragmentClass.newInstance();
                if (extras != null) {
                    fragment.setArguments(extras);
                }
                if (fragmentManager != null) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    if (fragmentTransaction != null) {
                        fragmentTransaction.replace(getRootViewId(), fragment);
                        fragmentTransaction.commitAllowingStateLoss();
                        pushFragment(new StackElement(TYPE.ROOT, connectedFragmentClass, extras));
                    }
                }
            } catch (Exception e) {
                Log.exception(e);
                finish();
            }
        });
    }
}
