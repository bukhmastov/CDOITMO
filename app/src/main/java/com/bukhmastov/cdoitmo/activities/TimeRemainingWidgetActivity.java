package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.objects.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONObject;

public class TimeRemainingWidgetActivity extends AppCompatActivity implements ScheduleLessons.response, TimeRemainingWidget.response {

    private static final String TAG = "TRWidgetActivity";
    private TimeRemainingWidget timeRemainingWidget = null;
    private ScheduleLessons scheduleLessons = null;
    private String query = null;
    private JSONObject schedule = null;
    private RequestHandle scheduleRequestHandle = null;
    private boolean is_message_displaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.darkTheme = Storage.pref.get(this, "pref_dark_theme", false);
        if (Static.darkTheme) setTheme(R.style.AppTheme_Popup_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_time_remaining_widget);
        try {
            String shortcut_data = getIntent().getStringExtra("shortcut_data");
            if (shortcut_data == null) throw new Exception("shortcut_data cannot be null");
            JSONObject json = new JSONObject(shortcut_data);
            query = json.getString("query");
            Log.v(TAG, "query=" + query);
        } catch (Exception e) {
            Static.error(e);
            close();
        }
        View trw_container = findViewById(R.id.trw_container);
        if (trw_container != null) {
            trw_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "trw_container clicked");
                    Intent intent = new Intent(getBaseContext(), SplashActivity.class);
                    intent.addFlags(Static.intentFlagRestart);
                    intent.putExtra("action", "schedule_lessons");
                    intent.putExtra("action_extra", query);
                    startActivity(intent);
                    close();
                }
            });
        }
        View time_remaining_widget = findViewById(R.id.time_remaining_widget);
        if (time_remaining_widget != null) {
            time_remaining_widget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "time_remaining_widget clicked");
                    close();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "Activity resumed");
        if (scheduleLessons == null) {
            scheduleLessons = new ScheduleLessons(this);
            scheduleLessons.setHandler(this);
        }
        if (schedule == null) {
            if (query != null) {
                scheduleLessons.search(query, true);
            } else {
                Log.w(TAG, "onResume | query is null");
                close();
            }
        } else {
            begin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "Activity paused");
        if (timeRemainingWidget != null) {
            timeRemainingWidget.stop();
            timeRemainingWidget = null;
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    @Override
    public void onProgress(int state) {
        Log.v(TAG, "progress " + state);
        message(getString(R.string.loading));
    }

    @Override
    public void onFailure(int state) {
        Log.v(TAG, "failure " + state);
        try {
            switch (state) {
                case IfmoRestClient.FAILED_OFFLINE:
                case ScheduleLessons.FAILED_OFFLINE:
                    message(getString(R.string.no_connection));
                    break;
                case IfmoRestClient.FAILED_TRY_AGAIN:
                case ScheduleLessons.FAILED_LOAD:
                    message(getString(R.string.load_failed));
                    break;
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public void onSuccess(JSONObject json) {
        Log.v(TAG, "success");
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            String type = json.getString("type");
            switch (type) {
                case "group":
                case "room":
                case "teacher": break;
                default: throw new Exception("json.type wrong value: " + type);
            }
            schedule = json;
            begin();
        } catch (Exception e) {
            Static.error(e);
            onFailure(ScheduleLessons.FAILED_LOAD);
        }
    }

    @Override
    public void onNewHandle(RequestHandle requestHandle) {
        scheduleRequestHandle = requestHandle;
    }

    @Override
    public void onAction(TimeRemainingWidget.Data data) {
        if (data.current == null && data.next == null && data.day == null) {
            message(getString(R.string.lessons_gone));
        } else {
            if (is_message_displaying) {
                draw(R.layout.layout_time_remaining_widget);
                is_message_displaying = false;
            }
            if (data.current != null) {
                setText(R.id.lesson_title, getString(R.string.current_lesson));
                setText(R.id.lesson_remaining, data.current);
            } else {
                setText(R.id.lesson_title, getString(R.string.next_lesson));
                if (data.next == null) {
                    setText(R.id.day_remaining, getString(R.string.unknown));
                } else {
                    setText(R.id.lesson_remaining, data.next);
                }
            }
            if (data.day == null) {
                setText(R.id.day_remaining, getString(R.string.unknown));
            } else {
                setText(R.id.day_remaining, data.day);
            }
        }
    }

    @Override
    public void onCancelled() {
        Log.v(TAG, "onCancelled");
        message(getString(R.string.widget_stopped));
    }

    private void begin(){
        Log.v(TAG, "begin");
        message(getString(R.string.loaded));
        if (timeRemainingWidget != null) {
            timeRemainingWidget.stop();
            timeRemainingWidget = null;
        }
        timeRemainingWidget = new TimeRemainingWidget(this);
        timeRemainingWidget.start(this, schedule);
    }
    private void close(){
        Log.v(TAG, "close");
        if (scheduleRequestHandle != null) {
            scheduleRequestHandle.cancel(true);
            scheduleRequestHandle = null;
        }
        finish();
    }
    private void setText(final int layout, final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(layout);
                if (textView != null) {
                    textView.setText(text);
                }
            }
        });
    }
    private void message(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                draw(R.layout.layout_time_remaining_widget_message);
                is_message_displaying = true;
                TextView trw_message = (TextView) findViewById(R.id.trw_message);
                if (trw_message != null) {
                    trw_message.setText(text);
                }
            }
        });
    }
    private void draw(final int layoutId){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup vg = ((ViewGroup) findViewById(R.id.trw_container));
                    if (vg != null) {
                        vg.removeAllViews();
                        vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                } catch (Exception e){
                    Static.error(e);
                }
            }
        });
    }

}
