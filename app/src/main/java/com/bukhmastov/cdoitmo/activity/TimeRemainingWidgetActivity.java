package com.bukhmastov.cdoitmo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.dialog.BottomSheetDialog;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class TimeRemainingWidgetActivity extends AppCompatActivity implements ScheduleLessons.Handler, TimeRemainingWidget.Delegate {

    private static final String TAG = "TRWidgetActivity";
    private final Activity activity = this;
    private String query = null;
    private JSONObject schedule = null;
    private Client.Request requestHandle = null;
    private boolean is_message_displaying = false;
    private TimeRemainingWidget.Data data = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    StoragePref storagePref;
    @Inject
    TimeRemainingWidget timeRemainingWidget;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Theme theme;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    private void inject() {
        if (thread == null) {
            AppComponentProvider.getComponent().inject(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inject();
        switch (theme.getAppTheme(activity)) {
            case "light":
            default: setTheme(R.style.AppTheme_Popup); break;
            case "dark": setTheme(R.style.AppTheme_Popup_Dark); break;
            case "white": setTheme(R.style.AppTheme_Popup_White); break;
            case "black": setTheme(R.style.AppTheme_Popup_Black); break;
        }
        super.onCreate(savedInstanceState);
        log.i(TAG, "Activity created");
        firebaseAnalyticsProvider.logCurrentScreen(this);
        setContentView(R.layout.activity_widget_remaining);
        try {
            String shortcut_data = getIntent().getStringExtra("shortcut_data");
            if (shortcut_data == null) throw new Exception("shortcut_data cannot be null");
            JSONObject json = new JSONObject(shortcut_data);
            query = json.getString("query");
            log.v(TAG, "query=" + query);
        } catch (Exception e) {
            log.exception(e);
            close();
        }
        View wr_container = findViewById(R.id.wr_container);
        if (wr_container != null) {
            wr_container.setOnClickListener(v -> {
                log.v(TAG, "wr_container clicked");
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(App.intentFlagRestart);
                intent.putExtra("action", "schedule_lessons");
                intent.putExtra("action_extra", query);
                activity.startActivity(intent);
                close();
            });
        }
        View wr_share = findViewById(R.id.wr_share);
        if (wr_share != null) {
            wr_share.setOnClickListener(v -> thread.runOnUI(() -> {
                log.v(TAG, "wr_share clicked");
                share();
            }));
        }
        View widget_remaining = findViewById(R.id.widget_remaining);
        if (widget_remaining != null) {
            widget_remaining.setOnClickListener(v -> {
                log.v(TAG, "widget_remaining clicked");
                close();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log.i(TAG, "Activity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log.v(TAG, "Activity resumed");
        if (schedule == null) {
            if (query != null) {
                scheduleLessons.init(this).search(activity, query, true);
            } else {
                log.w(TAG, "onResume | query is null");
                close();
            }
        } else {
            begin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.v(TAG, "Activity paused");
        timeRemainingWidget.stop();
    }

    @Override
    protected void attachBaseContext(Context context) {
        inject();
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref, log, textUtils));
    }

    @Override
    public void onProgress(int state) {
        log.v(TAG, "progress " + state);
        message(activity.getString(R.string.loading));
    }

    @Override
    public void onFailure(int state) {
        this.onFailure(0, null, state);
    }

    @Override
    public void onFailure(int statusCode, Client.Headers headers, int state) {
        log.v(TAG, "failure " + state);
        try {
            switch (state) {
                case IfmoRestClient.FAILED_OFFLINE:
                case ScheduleLessons.FAILED_OFFLINE:
                    message(activity.getString(R.string.no_connection));
                    break;
                case IfmoRestClient.FAILED_SERVER_ERROR:
                    message(IfmoRestClient.getFailureMessage(activity, statusCode));
                    break;
                case IfmoRestClient.FAILED_CORRUPTED_JSON:
                    message(activity.getString(R.string.server_provided_corrupted_json));
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
            log.exception(e);
        }
    }

    @Override
    public void onSuccess(JSONObject json, boolean fromCache) {
        log.v(TAG, "success");
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
            log.exception(e);
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
        this.data = data;
        if (data.current == null && data.next == null && data.day == null) {
            message(activity.getString(R.string.lessons_gone));
        } else {
            thread.runOnUI(() -> {
                if (is_message_displaying) {
                    draw(R.layout.widget_remaining_time);
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
        log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        thread.run(() -> {
            log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            timeRemainingWidget.stop();
            timeRemainingWidget.start(this, this, schedule);
        });
    }
    private void close() {
        thread.run(() -> {
            log.v(TAG, "close");
            if (requestHandle != null) {
                requestHandle.cancel();
            }
            finish();
        });
    }
    private void setText(final int layout, final String text) {
        thread.runOnUI(() -> {
            TextView textView = activity.findViewById(layout);
            if (textView != null) {
                textView.setText(text);
            }
        });
    }
    private void message(final String text) {
        thread.runOnUI(() -> {
            draw(R.layout.widget_remaining_message);
            is_message_displaying = true;
            TextView message = activity.findViewById(R.id.message);
            if (message != null) {
                message.setText(text);
            }
        });
    }
    private void share() {
        thread.runOnUI(() -> {
            if (data == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.share_unable));
                return;
            }
            if (data.current == null && data.next == null && data.day == null) {
                share(activity.getString(R.string.time_remaining_widget_share_1));
                return;
            }
            new BottomSheetDialog(
                    activity,
                    activity.getString(R.string.share),
                    new BottomSheetDialog.Entry(data.current != null ? activity.getString(R.string.current_lesson) : null, "current"),
                    new BottomSheetDialog.Entry(data.next != null ? activity.getString(R.string.next_lesson) : null, "next"),
                    new BottomSheetDialog.Entry(data.day != null ? activity.getString(R.string.lessons_day_end) : null, "day")
            ).setListener(tag -> {
                switch (tag) {
                    case "current":
                        share(activity.getString(R.string.time_remaining_widget_share_2) + " " + time2readable(data.current));
                        break;
                    case "next":
                        share(activity.getString(R.string.time_remaining_widget_share_3) + " " + time2readable(data.next));
                        break;
                    case "day":
                        share(activity.getString(R.string.time_remaining_widget_share_4) + " " + time2readable(data.day));
                        break;
                }
            }).show();
        });
    }
    private void share(String text) {
        if (!text.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share)));
            // track statistics
            firebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "time_remaining_widget")
            );
        }
    }
    private String time2readable(String time) {
        if (time == null) {
            return "";
        }
        String suffix;
        Matcher m = Pattern.compile("^(\\d*:?)(\\d*:?)?(\\d*:?)?$").matcher(time);
        int elements = time.split(":").length;
        if (m.find()) {
            time = "";
            if (elements > 2) {
                int hour = Integer.parseInt(m.group(elements - 2).replace(":", ""));
                suffix = "ов";
                if (hour % 100 < 10 || hour % 100 > 20) {
                    switch (hour % 10) {
                        case 1: suffix = ""; break;
                        case 2: case 3: case 4: suffix = "а"; break;
                    }
                }
                time += " " + hour + " час" + suffix;
            }
            if (elements > 1) {
                int min = Integer.parseInt(m.group(elements - 1).replace(":", ""));
                suffix = "";
                if (min % 100 < 10 || min % 100 > 20) {
                    switch (min % 10) {
                        case 1: suffix = "у"; break;
                        case 2: case 3: case 4: suffix = "ы"; break;
                    }
                }
                time += " " + min + " минут" + suffix;
            }
            if (elements > 0) {
                int sec = Integer.parseInt(m.group(elements).replace(":", ""));
                suffix = "";
                if (sec % 100 < 10 || sec % 100 > 20) {
                    switch (sec % 10) {
                        case 1: suffix = "у"; break;
                        case 2: case 3: case 4: suffix = "ы"; break;
                    }
                }
                time += (time.isEmpty() ? " " : " и ") + sec + " секунд" + suffix;
            }
        }
        return time.trim();
    }

    private void draw(final int layoutId) {
        try {
            ViewGroup vg = activity.findViewById(R.id.wr_container);
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        } catch (Exception e){
            log.exception(e);
        }
    }
    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
