package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.model.entity.ShortcutQuery;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.DaysRemainingWidget;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.WDR;

public class DaysRemainingWidgetActivityPresenterImpl implements DaysRemainingWidgetActivityPresenter, ScheduleExams.Handler<SExams>, DaysRemainingWidget.Delegate {

    private static final String TAG = "DRWidgetActivity";
    private DaysRemainingWidgetActivity activity = null;
    private String query = null;
    private SExams schedule = null;
    private Client.Request requestHandle = null;
    private Boolean isMessageDisplaying = null;
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
        thread.initialize(WDR);
        thread.runOnUI(WDR, () -> {
            log.i(TAG, "Activity created");
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            String shortcutData = activity.getIntent().getStringExtra("shortcut_data");
            if (StringUtils.isBlank(shortcutData)) {
                log.w(TAG, "shortcutData is blank");
                close();
                return;
            }
            ShortcutQuery shortcutQuery = new ShortcutQuery().fromJsonString(shortcutData);
            if (shortcutQuery == null || StringUtils.isBlank(shortcutQuery.getQuery())) {
                log.w(TAG, "shortcutQuery.getQuery() is blank");
                close();
                return;
            }
            isMessageDisplaying = null;
            query = shortcutQuery.getQuery();
            log.v(TAG, "query=", query);
            View container = activity.findViewById(R.id.wr_container);
            if (container != null) {
                container.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "schedule_exams");
                    bundle.putString("action_extra", query);
                    eventBus.fire(new OpenActivityEvent(MainActivity.class, bundle, App.intentFlagRestart));
                    close();
                });
            }
            View share = activity.findViewById(R.id.wr_share);
            if (share != null) {
                share.setOnClickListener(v -> share());
            }
            View remaining = activity.findViewById(R.id.widget_remaining);
            if (remaining != null) {
                remaining.setOnClickListener(v -> close());
            }
        });
    }

    @Override
    public void onResume() {
        thread.run(WDR, () -> {
            log.v(TAG, "Activity resumed");
            if (schedule != null) {
                begin();
                return;
            }
            if (query != null) {
                scheduleExams.search(query, true, this);
                return;
            }
            log.w(TAG, "onResume | query is null");
            close();
        });
    }

    @Override
    public void onPause() {
        thread.run(WDR, () -> {
            log.v(TAG, "Activity paused");
            daysRemainingWidget.stop();
        });
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
        thread.interrupt(WDR);
    }

    @Override
    public void onProgress(int state) {
        log.v(TAG, "progress ", state);
        message(activity.getString(R.string.loading));
    }

    @Override
    public void onSuccess(SExams data, boolean fromCache) {
        thread.run(WDR, () -> {
            log.v(TAG, "success");
            if (data == null || StringUtils.isBlank(data.getType())) {
                log.w(TAG, "onSuccess | schedule cannot be null");
                onFailure(ScheduleExams.FAILED_LOAD);
                return;
            }
            switch (data.getType()) {
                case "group":
                case "teacher": break;
                default:
                    log.w(TAG, "onSuccess | schedule.getType() wrong value: " + data.getType());
                    onFailure(ScheduleExams.FAILED_LOAD);
                    return;
            }
            schedule = data;
            begin();
        }, throwable -> {
            log.exception(throwable);
            onFailure(ScheduleExams.FAILED_LOAD);
        });
    }

    @Override
    public void onFailure(int code, Client.Headers headers, int state) {
        log.v(TAG, "failure " + state);
        switch (state) {
            case Client.FAILED_OFFLINE:
            case ScheduleExams.FAILED_OFFLINE:
                message(activity.getString(R.string.no_connection));
                break;
            case Client.FAILED_SERVER_ERROR:
                message(Client.getFailureMessage(activity, code));
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
            case ScheduleExams.FAILED_PERSONAL_NEED_ISU:
                message(activity.getString(R.string.load_failed_need_isu));
                break;
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
        thread.runOnUI(WDR, () -> {
            this.data = data;
            if (data.size() == 0) {
                message(activity.getString(R.string.exams_gone));
            } else {
                if (isMessageDisplaying == null || isMessageDisplaying) {
                    draw(R.layout.widget_remaining_days);
                    isMessageDisplaying = false;
                }
                DaysRemainingWidget.Data currentData = data.get(0);
                setText(R.id.exam_subject, currentData.subject);
                setText(R.id.exam_teacher, currentData.desc);
                setText(R.id.exam_time_day, R.id.exam_time_day_value, currentData.time.day);
                setText(R.id.exam_time_hour, R.id.exam_time_hour_value, currentData.time.hour);
                setText(R.id.exam_time_min, R.id.exam_time_min_value, currentData.time.min);
                setText(R.id.exam_time_sec, R.id.exam_time_sec_value, currentData.time.sec);
            }
        });
    }

    @Override
    public void onCancelled() {
        log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        thread.run(WDR, () -> {
            log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            daysRemainingWidget.stop();
            daysRemainingWidget.start(schedule, this);
        });
    }

    private void close() {
        log.v(TAG, "close");
        thread.standalone(() -> {
            if (requestHandle != null) {
                requestHandle.cancel();
            }
        });
        thread.runOnUI(WDR, () -> activity.finish());
    }

    private void message(String text) {
        thread.runOnUI(WDR, () -> {
            if (isMessageDisplaying == null || !isMessageDisplaying) {
                draw(R.layout.widget_remaining_message);
                isMessageDisplaying = true;
            }
            TextView message = activity.findViewById(R.id.message);
            if (message != null) {
                message.setText(text);
            }
        });
    }

    private void share() {
        thread.run(WDR, () -> {
            if (data == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.share_unable));
                return;
            }
            if (data.size() == 0) {
                eventBus.fire(new ShareTextEvent("У меня закончились экзамены!", "txt_widget_remaining_days"));
                return;
            }
            DaysRemainingWidget.Data currentData = data.get(0);
            if (currentData.subject == null || currentData.time == null || (
                    currentData.time.day == null && currentData.time.hour == null &&
                    currentData.time.min == null && currentData.time.sec == null
            )) {
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
            eventBus.fire(new ShareTextEvent(pattern, "txt_widget_remaining_days"));
        });
    }

    private void setText(int layout, String text) {
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
    }

    private void setText(int container, int layout, String text) {
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
    }

    private void draw(int layoutId) {
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
