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
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.TimeRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activity.presenter.TimeRemainingWidgetActivityPresenter;
import com.bukhmastov.cdoitmo.dialog.BottomSheetDialog;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.model.entity.ShortcutQuery;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.WTR;

public class TimeRemainingWidgetActivityPresenterImpl implements TimeRemainingWidgetActivityPresenter, ScheduleLessons.Handler<SLessons>, TimeRemainingWidget.Delegate {

    private static final String TAG = "TRWidgetActivity";
    private TimeRemainingWidgetActivity activity = null;
    private String query = null;
    private SLessons schedule = null;
    private Client.Request requestHandle = null;
    private Boolean isMessageDisplaying = null;
    private TimeRemainingWidget.Data data = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    TimeRemainingWidget timeRemainingWidget;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public TimeRemainingWidgetActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull TimeRemainingWidgetActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(WTR);
        thread.runOnUI(WTR, () -> {
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
                    bundle.putString("action", "schedule_lessons");
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
        }, throwable -> {
            log.exception(throwable);
            close();
        });
    }

    @Override
    public void onResume() {
        thread.run(WTR, () -> {
            log.v(TAG, "Activity resumed");
            if (schedule != null) {
                begin();
                return;
            }
            if (query != null) {
                scheduleLessons.search(query, true, this);
                return;
            }
            log.w(TAG, "onResume | query is null");
            close();
        });
    }

    @Override
    public void onPause() {
        thread.run(WTR, () -> {
            log.v(TAG, "Activity paused");
            timeRemainingWidget.stop();
        });
    }

    @Override
    public void onDestroy() {
        log.i(TAG, "Activity destroyed");
        thread.interrupt(WTR);
    }

    @Override
    public void onSuccess(SLessons data, boolean fromCache) {
        thread.run(WTR, () -> {
            log.v(TAG, "success");
            if (data == null || StringUtils.isBlank(data.getType())) {
                log.w(TAG, "onSuccess | schedule cannot be null");
                onFailure(ScheduleLessons.FAILED_LOAD);
                return;
            }
            switch (data.getType()) {
                case "group":
                case "room":
                case "teacher": break;
                default:
                    log.w(TAG, "onSuccess | schedule.getType() wrong value: " + data.getType());
                    onFailure(ScheduleLessons.FAILED_LOAD);
                    return;
            }
            schedule = data;
            begin();
        }, throwable -> {
            log.exception(throwable);
            onFailure(ScheduleLessons.FAILED_LOAD);
        });
    }

    @Override
    public void onFailure(int code, Client.Headers headers, int state) {
        log.v(TAG, "failure " + state);
        switch (state) {
            case IfmoRestClient.FAILED_OFFLINE:
            case ScheduleLessons.FAILED_OFFLINE:
                message(activity.getString(R.string.no_connection));
                break;
            case IfmoRestClient.FAILED_SERVER_ERROR:
                message(IfmoRestClient.getFailureMessage(activity, code));
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
            case ScheduleLessons.FAILED_PERSONAL_NEED_ISU:
                message(activity.getString(R.string.load_failed_need_isu));
                break;
        }
    }

    @Override
    public void onProgress(int state) {
        log.v(TAG, "progress " + state);
        message(activity.getString(R.string.loading));
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
    public void onAction(TimeRemainingWidget.Data data) {
        thread.runOnUI(WTR, () -> {
            this.data = data;
            if (data.current == null && data.next == null && data.day == null) {
                message(activity.getString(R.string.lessons_gone));
                return;
            }
            if (isMessageDisplaying == null || isMessageDisplaying) {
                draw(R.layout.widget_remaining_time);
                isMessageDisplaying = false;
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
            View currentLesson15min = activity.findViewById(R.id.current_lesson_15min);
            View currentLesson15minSeparator = activity.findViewById(R.id.current_lesson_15min_separator);
            if (data.current15min != null) {
                if (currentLesson15min != null && currentLesson15minSeparator != null && currentLesson15min.getVisibility() == View.GONE) {
                    currentLesson15min.setVisibility(View.VISIBLE);
                    currentLesson15minSeparator.setVisibility(View.VISIBLE);
                }
                setText(R.id.lesson_15min_remaining, data.current15min);
            } else if (currentLesson15min != null && currentLesson15minSeparator != null && currentLesson15min.getVisibility() == View.VISIBLE) {
                currentLesson15min.setVisibility(View.GONE);
                currentLesson15minSeparator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onCancelled() {
        log.v(TAG, "onCancelled");
        message(activity.getString(R.string.widget_stopped));
    }

    private void begin() {
        thread.run(WTR, () -> {
            log.v(TAG, "begin");
            message(activity.getString(R.string.loaded));
            timeRemainingWidget.stop();
            timeRemainingWidget.start(schedule, this);
        });
    }

    private void close() {
        log.v(TAG, "close");
        thread.standalone(() -> {
            if (requestHandle != null) {
                requestHandle.cancel();
            }
        });
        thread.runOnUI(WTR, () -> activity.finish());
    }

    private void message(String text) {
        thread.runOnUI(WTR, () -> {
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
        thread.runOnUI(WTR, () -> {
            if (data == null) {
                notificationMessage.snackBar(activity, activity.getString(R.string.share_unable));
                return;
            }
            if (data.current == null && data.next == null && data.day == null) {
                eventBus.fire(new ShareTextEvent(activity.getString(R.string.time_remaining_widget_share_1), "txt_widget_remaining_time"));
                return;
            }
            new BottomSheetDialog(
                    activity,
                    activity.getString(R.string.share),
                    new BottomSheetDialog.Entry(data.current != null ? activity.getString(R.string.current_lesson) : null, "current"),
                    new BottomSheetDialog.Entry(data.next != null ? activity.getString(R.string.next_lesson) : null, "next"),
                    new BottomSheetDialog.Entry(data.day != null ? activity.getString(R.string.lessons_day_end) : null, "day")
            ).setListener(tag -> thread.standalone(() -> {
                switch (tag) {
                    case "current":
                        eventBus.fire(new ShareTextEvent(activity.getString(R.string.time_remaining_widget_share_2) + " " + time2readable(data.current), "txt_widget_remaining_time"));
                        break;
                    case "next":
                        eventBus.fire(new ShareTextEvent(activity.getString(R.string.time_remaining_widget_share_3) + " " + time2readable(data.next), "txt_widget_remaining_time"));
                        break;
                    case "day":
                        eventBus.fire(new ShareTextEvent(activity.getString(R.string.time_remaining_widget_share_4) + " " + time2readable(data.day), "txt_widget_remaining_time"));
                        break;
                }
            })).show();
        });
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

    private void setText(int layout, String text) {
        TextView textView = activity.findViewById(layout);
        if (textView != null) {
            textView.setText(text);
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
