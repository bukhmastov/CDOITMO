package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
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
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONObject;

public class TimeRemainingWidgetActivity extends AppCompatActivity implements ScheduleLessons.Handler, TimeRemainingWidget.response {

    private static final String TAG = "TRWidgetActivity";
    private final Activity activity = this;
    private TimeRemainingWidget timeRemainingWidget = null;
    private ScheduleLessons scheduleLessons = null;
    private String query = null;
    private JSONObject schedule = null;
    private Client.Request requestHandle = null;
    private boolean is_message_displaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        switch (Static.getAppTheme(activity)) {
            case "light":
            default: setTheme(R.style.AppTheme_Popup); break;
            case "dark": setTheme(R.style.AppTheme_Popup_Dark); break;
            case "white": setTheme(R.style.AppTheme_Popup_White); break;
            case "black": setTheme(R.style.AppTheme_Popup_Black); break;
        }
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
            trw_container.setOnClickListener(v -> {
                Log.v(TAG, "trw_container clicked");
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Static.intentFlagRestart);
                intent.putExtra("action", "schedule_lessons");
                intent.putExtra("action_extra", query);
                startActivity(intent);
                close();
            });
        }
        View time_remaining_widget = findViewById(R.id.time_remaining_widget);
        if (time_remaining_widget != null) {
            time_remaining_widget.setOnClickListener(v -> {
                Log.v(TAG, "time_remaining_widget clicked");
                close();
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
        }
        if (schedule == null) {
            if (query != null) {
                scheduleLessons.search(activity, query, true);
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
        message(activity.getString(R.string.loading));
    }

    @Override
    public void onFailure(int state) {
        this.onFailure(0, null, state);
    }

    @Override
    public void onFailure(int statusCode, Client.Headers headers, int state) {
        Log.v(TAG, "failure " + state);
        try {
            switch (state) {
                case IfmoRestClient.FAILED_OFFLINE:
                case ScheduleLessons.FAILED_OFFLINE:
                    message(activity.getString(R.string.no_connection));
                    break;
                case IfmoRestClient.FAILED_SERVER_ERROR:
                    message(IfmoRestClient.getFailureMessage(activity, statusCode));
                    break;
                case IfmoRestClient.FAILED_TRY_AGAIN:
                case ScheduleLessons.FAILED_LOAD:
                case ScheduleLessons.FAILED_EMPTY_QUERY:
                    message(activity.getString(R.string.load_failed));
                    break;
                case ScheduleLessons.FAILED_NOT_FOUND:
                    message(activity.getString(R.string.no_schedule));
                    break;
                case ScheduleLessons.FAILED_INVALID_QUERY:
                    message(activity.getString(R.string.incorrect_query));
                    break;
                case ScheduleLessons.FAILED_MINE_NEED_ISU:
                    // TODO replace with isu auth, when isu will be ready
                    message(activity.getString(R.string.load_failed));
                    break;
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    @Override
    public void onSuccess(JSONObject json, boolean fromCache) {
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
    public void onNewRequest(Client.Request request) {
        requestHandle = request;
    }

    @Override
    public void onCancelRequest() {
        if (requestHandle != null) {
            requestHandle.cancel();
        }
    }

    @Override
    public void onAction(final TimeRemainingWidget.Data data) {
        if (data.current == null && data.next == null && data.day == null) {
            message(activity.getString(R.string.lessons_gone));
        } else {
            Static.T.runOnUiThread(() -> {
                if (is_message_displaying) {
                    draw(R.layout.layout_time_remaining_widget);
                    is_message_displaying = false;
                }
                if (data.current != null) {
                    setText(R.id.lesson_title, activity.getString(R.string.current_lesson));
                    setText(R.id.lesson_remaining, data.current);
                } else {
                    setText(R.id.lesson_title, activity.getString(R.string.next_lesson));
                    if (data.next == null) {
                        setText(R.id.day_remaining, activity.getString(R.string.unknown));
                    } else {
                        setText(R.id.lesson_remaining, data.next);
                    }
                }
                if (data.day == null) {
                    setText(R.id.day_remaining, activity.getString(R.string.unknown));
                } else {
                    setText(R.id.day_remaining, data.day);
                }
                View current_lesson_15min = activity.findViewById(R.id.current_lesson_15min);
                View current_lesson_15min_separator = activity.findViewById(R.id.current_lesson_15min_separator);
                if (data.current_15min != null) {
                    if (current_lesson_15min != null && current_lesson_15min_separator != null && current_lesson_15min.getVisibility() == View.GONE) {
                        current_lesson_15min.setVisibility(View.VISIBLE);
                        current_lesson_15min_separator.setVisibility(View.VISIBLE);
                    }
                    setText(R.id.lesson_15min_remaining, data.current_15min);
                } else {
                    if (current_lesson_15min != null && current_lesson_15min_separator != null && current_lesson_15min.getVisibility() == View.VISIBLE) {
                        current_lesson_15min.setVisibility(View.GONE);
                        current_lesson_15min_separator.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    @Override
    public void onCancelled() {
        Log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        final TimeRemainingWidgetActivity self = this;
        Static.T.runThread(() -> {
            Log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            if (timeRemainingWidget != null) {
                timeRemainingWidget.stop();
                timeRemainingWidget = null;
            }
            timeRemainingWidget = new TimeRemainingWidget(self);
            timeRemainingWidget.start(self, schedule);
        });
    }
    private void close() {
        Static.T.runThread(() -> {
            Log.v(TAG, "close");
            if (requestHandle != null) {
                requestHandle.cancel();
            }
            finish();
        });
    }
    private void setText(final int layout, final String text) {
        Static.T.runOnUiThread(() -> {
            TextView textView = activity.findViewById(layout);
            if (textView != null) {
                textView.setText(text);
            }
        });
    }
    private void message(final String text) {
        Static.T.runOnUiThread(() -> {
            draw(R.layout.layout_time_remaining_widget_message);
            is_message_displaying = true;
            TextView trw_message = activity.findViewById(R.id.trw_message);
            if (trw_message != null) {
                trw_message.setText(text);
            }
        });
    }
    private void draw(final int layoutId) {
        try {
            ViewGroup vg = activity.findViewById(R.id.trw_container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            Static.error(e);
        }
    }
}
