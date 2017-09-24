package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class FileReceiveActivity extends ConnectedActivity {

    private static final String TAG = "FileReceiveActivity";
    private final Activity activity = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Static.init(activity);
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_file_receive);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_file));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Proceed received file
        try {
            final Intent intent = activity.getIntent();
            final String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_VIEW: {
                    final String type = intent.getType();
                    switch (type) {
                        case "application/cdoitmo": {
                            final Uri data = intent.getData();
                            if (data == null) {
                                throw new MessageException(activity.getString(R.string.error_while_handle_file));
                            }
                            final String file = Static.readFileFromUri(activity, data);
                            final JSONObject object = (JSONObject) new JSONTokener(file).nextValue();
                            switch (object.getString("type")) {
                                case "share_schedule_of_lessons": {
                                    share_schedule_of_lessons(file, object);
                                    break;
                                }
                                default: {
                                    throw new MessageException(activity.getString(R.string.file_doesnot_supported));
                                }
                            }
                            break;
                        }
                        default: {
                            throw new MessageException(activity.getString(R.string.file_doesnot_supported));
                        }
                    }
                    break;
                }
                default: {
                    throw new MessageException(activity.getString(R.string.failed_to_handle_file));
                }
            }
        } catch (MessageException e) {
            failure(e.getMessage());
        } catch (Throwable throwable) {
            failure(activity.getString(R.string.failed_to_handle_file));
        } finally {
            try {
                Intent intent = activity.getIntent();
                intent.setAction(Intent.ACTION_MAIN);
                intent.setType(null);
                intent.setData(null);
                activity.setIntent(intent);
            } catch (Throwable ignore) {
                // ignore
            }
        }
    }

    private void share_schedule_of_lessons(final String file, final JSONObject object) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "share_schedule_of_lessons");
                    if (Storage.file.general.get(activity, "users#current_login", "").trim().isEmpty()) {
                        throw new MessageException(activity.getString(R.string.file_requires_auth));
                    }
                    if (object.has("content")) {
                        final JSONObject content = object.getJSONObject("content");
                        if (
                                !(content.has("query") && content.get("query") instanceof String) ||
                                !(content.has("title") && content.get("title") instanceof String) ||
                                !(content.has("token") && content.get("token") instanceof String) ||
                                !(content.has("added") && content.get("added") instanceof JSONArray) ||
                                !(content.has("reduced") && content.get("reduced") instanceof JSONArray)
                        ) {
                            throw new MessageException(activity.getString(R.string.corrupted_file));
                        }
                    } else {
                        throw new MessageException(activity.getString(R.string.corrupted_file));
                    }
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Bundle extras = new Bundle();
                            extras.putString("type", "handle");
                            extras.putString("data", file);
                            if (!openFragment(TYPE.root, ScheduleLessonsShareFragment.class, extras)) {
                                failure(activity.getString(R.string.failed_to_display_file));
                            }
                        }
                    });
                } catch (MessageException e) {
                    failure(e.getMessage());
                } catch (Exception e) {
                    failure(activity.getString(R.string.failed_to_decode_file));
                }
            }
        });
    }

    private void failure(final String message) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failure | message=" + message);
                View state_failed_without_align = inflate(R.layout.state_failed_without_align);
                ((TextView) state_failed_without_align.findViewById(R.id.text)).setText(message);
                ViewGroup container = activity.findViewById(getRootViewId());
                container.removeAllViews();
                container.addView(state_failed_without_align);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (back()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getRootViewId() {
        return R.id.activity_file_receive;
    }

    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }

    private class MessageException extends Exception {
        private MessageException(String message) {
            super(message);
        }
    }
}
