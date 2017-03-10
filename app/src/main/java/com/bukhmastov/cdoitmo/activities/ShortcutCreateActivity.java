package com.bukhmastov.cdoitmo.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.objects.ShortcutCreator;
import com.bukhmastov.cdoitmo.utils.Static;

public class ShortcutCreateActivity extends AppCompatActivity implements ShortcutCreator.response {

    private static final String TAG = "ShortcutCreateActivity";
    private ShortcutCreator shortcutCreator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcut_create);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_shortcut_create));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.add_shortcut);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shortcutCreator == null) shortcutCreator = new ShortcutCreator(this, this);
        shortcutCreator.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shortcutCreator.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDisplay(View view) {
        ViewGroup shortcut_create_content = (ViewGroup) findViewById(R.id.shortcut_create_content);
        if (shortcut_create_content != null) {
            shortcut_create_content.removeAllViews();
            shortcut_create_content.addView(view);
        }
    }

}
