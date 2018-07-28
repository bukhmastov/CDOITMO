package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.DaysRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.presenter.DaysRemainingWidgetActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.DaysRemainingWidget;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public class DaysRemainingWidgetActivityPresenterImpl implements DaysRemainingWidgetActivityPresenter, ScheduleExams.Handler, DaysRemainingWidget.Delegate {

    private static final String TAG = "DRWidgetActivity";
    private DaysRemainingWidgetActivity activity = null;
    private String query = null;
    private JSONObject schedule = null;
    private Client.Request requestHandle = null;
    private boolean is_message_displaying = false;
    private final LinearLayout.LayoutParams hide = new LinearLayout.LayoutParams(0, 0);
    private final LinearLayout.LayoutParams show = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private final LinearLayout.LayoutParams showMatch = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private ArrayList<DaysRemainingWidget.Data> data = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleExams scheduleExams;
    @Inject
    DaysRemainingWidget daysRemainingWidget;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public DaysRemainingWidgetActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull DaysRemainingWidgetActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.i(TAG, "Activity created");
        firebaseAnalyticsProvider.logCurrentScreen(activity);
        try {
            String shortcut_data = activity.getIntent().getStringExtra("shortcut_data");
            if (shortcut_data == null) throw new Exception("shortcut_data cannot be null");
            JSONObject json = new JSONObject(shortcut_data);
            query = json.getString("query");
            log.v(TAG, "query=" + query);
        } catch (Exception e) {
            log.exception(e);
            close();
        }
        View wr_container = activity.findViewById(R.id.wr_container);
        if (wr_container != null) {
            wr_container.setOnClickListener(v -> {
                log.v(TAG, "wr_container clicked");
                Bundle bundle = new Bundle();
                bundle.putString("action", "schedule_exams");
                bundle.putString("action_extra", query);
                eventBus.fire(new OpenActivityEvent(MainActivity.class, bundle, App.intentFlagRestart));
                close();
            });
        }
        View wr_share = activity.findViewById(R.id.wr_share);
        if (wr_share != null) {
            wr_share.setOnClickListener(v -> thread.runOnUI(() -> {
                log.v(TAG, "wr_share clicked");
                share();
            }));
        }
        View widget_remaining = activity.findViewById(R.id.widget_remaining);
        if (widget_remaining != null) {
            widget_remaining.setOnClickListener(v -> {
                log.v(TAG, "widget_remaining clicked");
                close();
            });
        }
    }

    @Override
    public void onResume() {
        log.v(TAG, "Activity resumed");
        if (schedule == null) {
            if (query != null) {
                scheduleExams.init(this).search(activity, query, true);
            } else {
                log.w(TAG, "onResume | query is null");
                close();
            }
        } else {
            begin();
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Activity paused");
        daysRemainingWidget.stop();
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
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
                case "teacher": break;
                default: throw new Exception("json.type wrong value: " + type);
            }
            schedule = json;
            begin();
        } catch (Exception e) {
            log.exception(e);
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
        this.data = data;
        if (data.size() == 0) {
            message(activity.getString(R.string.exams_gone));
        } else {
            if (is_message_displaying) {
                draw(R.layout.widget_remaining_days);
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
        log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        thread.run(() -> {
            log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            daysRemainingWidget.stop();
            daysRemainingWidget.start(activity, this, schedule);
        });
    }

    private void close() {
        thread.run(() -> {
            log.v(TAG, "close");
            if (requestHandle != null) {
                requestHandle.cancel();
            }
            activity.finish();
        });
    }

    private void setText(final int layout, final String text) {
        thread.runOnUI(() -> {
            TextView textView = activity.findViewById(layout);
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
        thread.runOnUI(() -> {
            try {
                if (text == null) {
                    View view = activity.findViewById(container);
                    if (view != null && (view.getLayoutParams() == show || view.getLayoutParams() != hide)) {
                        view.setLayoutParams(hide);
                    }
                } else {
                    View view = activity.findViewById(container);
                    if (view != null && (view.getLayoutParams() == hide || view.getLayoutParams() != show)) {
                        view.setLayoutParams(show);
                    }
                    TextView textView = activity.findViewById(layout);
                    if (textView != null) {
                        textView.setText(text);
                    }
                }
            } catch (Exception e) {
                log.exception(e);
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
            if (data.size() == 0) {
                share("У меня закончились экзамены!");
                return;
            }
            DaysRemainingWidget.Data currentData = data.get(0);
            if (currentData.subject == null || currentData.time == null || (currentData.time.day == null && currentData.time.hour == null && currentData.time.min == null && currentData.time.sec == null)) {
                notificationMessage.snackBar(activity, activity.getString(R.string.share_unable));
                return;
            }
            String time = "";
            try {
                if (currentData.time.sec != null) {
                    int sec = Integer.parseInt(currentData.time.sec);
                    String suffix = "";
                    if (sec % 100 < 10 || sec % 100 > 20) {
                        switch (sec % 10) {
                            case 1: suffix = "у"; break;
                            case 2: case 3: case 4: suffix = "ы"; break;
                        }
                    }
                    time = currentData.time.sec + " секунд" + suffix + " " + time;
                }
            } catch (Exception ignore) {/* ignore */}
            try {
                if (currentData.time.min != null) {
                    int min = Integer.parseInt(currentData.time.min);
                    String suffix = "";
                    if (min % 100 < 10 || min % 100 > 20) {
                        switch (min % 10) {
                            case 1: suffix = "у"; break;
                            case 2: case 3: case 4: suffix = "ы"; break;
                        }
                    }
                    time = currentData.time.min + " минут" + suffix + " " + time;
                }
            } catch (Exception ignore) {/* ignore */}
            try {
                if (currentData.time.hour != null) {
                    int hour = Integer.parseInt(currentData.time.hour);
                    String suffix = "ов";
                    if (hour % 100 < 10 || hour % 100 > 20) {
                        switch (hour % 10) {
                            case 1: suffix = ""; break;
                            case 2: case 3: case 4: suffix = "а"; break;
                        }
                    }
                    time = currentData.time.hour + " час" + suffix + " " + time;
                }
            } catch (Exception ignore) {/* ignore */}
            try {
                if (currentData.time.day != null) {
                    int day = Integer.parseInt(currentData.time.day);
                    String suffix = "дней";
                    if (day % 100 < 10 || day % 100 > 20) {
                        switch (day % 10) {
                            case 1: suffix = "день"; break;
                            case 2: case 3: case 4: suffix = "дня"; break;
                        }
                    }
                    time = currentData.time.day + " " + suffix + " " + time;
                }
            } catch (Exception ignore) {/* ignore */}
            String scope = "";
            if (currentData.subject != null) {
                scope += " по предмету \"%subject%\"".replace("%subject%", currentData.subject);
            }
            if (currentData.desc != null) {
                scope += " (%desc%)".replace("%desc%", currentData.desc);
            }
            String pattern = "Следующий экзамен %scope% уже через %time%";
            pattern = pattern.replace("%scope%", scope.trim());
            pattern = pattern.replace("%time%", time.trim());
            share(pattern);
        });
    }

    private void share(String text) {
        if (!text.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            eventBus.fire(new OpenIntentEvent(Intent.createChooser(intent, activity.getString(R.string.share))));
            // track statistics
            firebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "days_remaining_widget")
            );
        }
    }

    private void draw(final int layoutId) {
        thread.runOnUI(() -> {
            try {
                ViewGroup vg = activity.findViewById(R.id.wr_container);
                if (vg != null) {
                    vg.removeAllViews();
                    vg.addView(inflate(layoutId), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            } catch (Exception e){
                log.exception(e);
            }
        });
    }

    private View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
