package com.bukhmastov.cdoitmo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.DaysRemainingWidget;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONObject;

import java.util.ArrayList;

public class DaysRemainingWidgetActivity extends AppCompatActivity implements ScheduleExams.Handler, DaysRemainingWidget.response {

    private static final String TAG = "DRWidgetActivity";
    private final Activity activity = this;
    private DaysRemainingWidget daysRemainingWidget = null;
    private ScheduleExams scheduleExams = null;
    private String query = null;
    private JSONObject schedule = null;
    private Client.Request requestHandle = null;
    private boolean is_message_displaying = false;
    private final LinearLayout.LayoutParams hide = new LinearLayout.LayoutParams(0, 0);
    private final LinearLayout.LayoutParams show = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private final LinearLayout.LayoutParams showMatch = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

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
        setContentView(R.layout.activity_days_remaining_widget);
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
        View drw_container = findViewById(R.id.drw_container);
        if (drw_container != null) {
            drw_container.setOnClickListener(v -> {
                Log.v(TAG, "drw_container clicked");
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Static.intentFlagRestart);
                intent.putExtra("action", "schedule_exams");
                intent.putExtra("action_extra", query);
                activity.startActivity(intent);
                close();
            });
        }
        View days_remaining_widget = findViewById(R.id.days_remaining_widget);
        if (days_remaining_widget != null) {
            days_remaining_widget.setOnClickListener(v -> {
                Log.v(TAG, "days_remaining_widget clicked");
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
        if (scheduleExams == null) {
            scheduleExams = new ScheduleExams(this);
        }
        if (schedule == null) {
            if (query != null) {
                scheduleExams.search(activity, query, true);
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
        if (daysRemainingWidget != null) {
            daysRemainingWidget.stop();
            daysRemainingWidget = null;
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
                case Client.FAILED_OFFLINE:
                case ScheduleExams.FAILED_OFFLINE:
                    message(activity.getString(R.string.no_connection));
                    break;
                case Client.FAILED_SERVER_ERROR:
                    message(Client.getFailureMessage(activity, statusCode));
                    break;
                case Client.FAILED_TRY_AGAIN:
                case ScheduleExams.FAILED_LOAD:
                case ScheduleExams.FAILED_EMPTY_QUERY:
                    message(activity.getString(R.string.load_failed));
                    break;
                case ScheduleExams.FAILED_NOT_FOUND:
                    message(activity.getString(R.string.no_schedule));
                    break;
                case ScheduleExams.FAILED_INVALID_QUERY:
                    message(activity.getString(R.string.incorrect_query));
                    break;
                case ScheduleExams.FAILED_MINE_NEED_ISU:
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
                case "teacher": break;
                default: throw new Exception("json.type wrong value: " + type);
            }
            schedule = json;
            begin();
        } catch (Exception e) {
            Static.error(e);
            onFailure(ScheduleExams.FAILED_LOAD);
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
    public void onAction(ArrayList<DaysRemainingWidget.Data> data) {
        if (data.size() == 0) {
            message(activity.getString(R.string.exams_gone));
        } else {
            if (is_message_displaying) {
                draw(R.layout.layout_days_remaining_widget);
                is_message_displaying = false;
            }
            DaysRemainingWidget.Data currentData = data.get(0);
            setText(R.id.exam_subject, currentData.subject);
            setText(R.id.exam_teacher, currentData.desc);
            setText(R.id.exam_time_day, R.id.exam_time_day_value, currentData.time.day);
            setText(R.id.exam_time_hour, R.id.exam_time_hour_value, currentData.time.hour);
            setText(R.id.exam_time_min, R.id.exam_time_min_value, currentData.time.min);
            setText(R.id.exam_time_sec, R.id.exam_time_sec_value, currentData.time.sec);
        }
    }

    @Override
    public void onCancelled() {
        Log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        final DaysRemainingWidgetActivity self = this;
        Static.T.runThread(() -> {
            Log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            if (daysRemainingWidget != null) {
                daysRemainingWidget.stop();
                daysRemainingWidget = null;
            }
            daysRemainingWidget = new DaysRemainingWidget(self);
            daysRemainingWidget.start(self, schedule);
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
            TextView textView = findViewById(layout);
            if (textView != null) {
                if (text == null || text.isEmpty()) {
                    if (textView.getLayoutParams() == showMatch || textView.getLayoutParams() != hide) {
                        textView.setLayoutParams(hide);
                    }
                } else {
                    textView.setText(text);
                    if (textView.getLayoutParams() == hide || textView.getLayoutParams() != showMatch) {
                        textView.setLayoutParams(showMatch);
                    }
                }
            }
        });
    }
    private void setText(final int container, final int layout, final String text) {
        Static.T.runOnUiThread(() -> {
            try {
                if (text == null) {
                    View view = findViewById(container);
                    if (view != null && (view.getLayoutParams() == show || view.getLayoutParams() != hide)) {
                        view.setLayoutParams(hide);
                    }
                } else {
                    View view = findViewById(container);
                    if (view != null && (view.getLayoutParams() == hide || view.getLayoutParams() != show)) {
                        view.setLayoutParams(show);
                    }
                    TextView textView = findViewById(layout);
                    if (textView != null) {
                        textView.setText(text);
                    }
                }
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    private void message(final String text) {
        Static.T.runOnUiThread(() -> {
            draw(R.layout.layout_days_remaining_widget_message);
            is_message_displaying = true;
            TextView drw_message = findViewById(R.id.drw_message);
            if (drw_message != null) {
                drw_message.setText(text);
            }
        });
    }
    private void draw(final int layoutId) {
        Static.T.runOnUiThread(() -> {
            try {
                ViewGroup vg = findViewById(R.id.drw_container);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            } catch (Exception e){
                Static.error(e);
            }
        });
    }
}
