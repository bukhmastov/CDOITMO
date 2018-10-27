package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.exception.CorruptedFileException;
import com.bukhmastov.cdoitmo.exception.MessageException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsShareFragmentPresenter;
import com.bukhmastov.cdoitmo.model.fileshare.FShare;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLAddedDay;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLReduced;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLReducedDay;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLessons;
import com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons.FSLessonsContent;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.lessons.added.SLessonsAdded;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SDayReduced;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SLessonsReduced;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import javax.inject.Inject;

public class ScheduleLessonsShareFragmentPresenterImpl implements ScheduleLessonsShareFragmentPresenter {

    private static final String TAG = "SLShareFragment";
    private static final String TYPE = "share_schedule_of_lessons";
    private static final int VERSION = 3;
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    
    private static class Change {
        private final @TYPE String type;
        private final int weekday;
        private final SLesson lesson;
        private boolean enabled;
        private Change(@TYPE String type, boolean enabled, int weekday, SLesson lesson) {
            this.type = type;
            this.enabled = enabled;
            this.weekday = weekday;
            this.lesson = lesson;
        }
    }
    
    private boolean keepGoing = true;
    private String action = "";
    private String query = "";
    private String type = "";
    private String title = "";
    private final ArrayList<Change> changes = new ArrayList<>();
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1, colorScheduleFlagIwsBG = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;
    @Inject
    Storage storage;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    
    public ScheduleLessonsShareFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            action = fragment.extras().getString("action");
            log.v(TAG, "Fragment created | action=" + action);
            if (action == null || !(action.equals("share") || action.equals("handle"))) {
                keepGoing = false;
                notificationMessage.toast(activity, activity.getString(R.string.corrupted_data));
                finish();
            }
            switch (action) {
                case "share":
                default: {
                    activity.updateToolbar(activity, activity.getString(R.string.share_changes), R.drawable.ic_share);
                    break;
                }
                case "handle": {
                    activity.updateToolbar(activity, activity.getString(R.string.accept_changes), R.drawable.ic_share);
                    break;
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        thread.run(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            changes.clear();
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            if (!keepGoing) {
                return;
            }
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (loaded) {
                return;
            }
            loaded = true;
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                throw new NullPointerException("extras cannot be null");
            }
            load(extras);
        }, throwable -> {
            log.exception(throwable);
            finish();
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(TAG, "Fragment paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public String getAction() {
        return action;
    }

    private void load(final Bundle extras) {
        thread.run(() -> {
            log.v(TAG, "load | action=", action);
            switch (action) {
                case "handle": loadHandle(extras); break;
                case "share": loadShare(extras); break;
                default: throw new Exception();
            }
        }, throwable -> {
            if (throwable instanceof CorruptedFileException) {
                log.v(TAG, "CorruptedFileException");
                notificationMessage.toast(activity, activity.getString(R.string.corrupted_file));
            } else if (throwable instanceof MessageException) {
                log.v(TAG, "MessageException: ", throwable.getMessage());
                notificationMessage.toast(activity, throwable.getMessage());
            } else {
                log.w(TAG, "Throwable: ", throwable.getMessage());
                notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            }
            finish();
        });
    }
    
    private void loadHandle(final Bundle extras) throws Exception {
        Serializable serializable = extras.getSerializable("data");
        if (!(serializable instanceof FSLessons)) {
            throw new IllegalArgumentException("Extra(data) not instanceof FSLessons");
        }
        FSLessons fsLessons = (FSLessons) serializable;
        if (fsLessons.getContent() == null) {
            throw new CorruptedFileException();
        }
        if (fsLessons.getVersion() < 2) {
            throw new MessageException(activity.getString(R.string.outdated_file_version));
        }
        FSLessonsContent fsLessonsContent = fsLessons.getContent();
        if (StringUtils.isBlank(fsLessonsContent.getQuery()) ||
            StringUtils.isBlank(fsLessonsContent.getTitle()) ||
            StringUtils.isBlank(fsLessonsContent.getType()) ||
            (CollectionUtils.isEmpty(fsLessonsContent.getReduced()) && CollectionUtils.isEmpty(fsLessonsContent.getAdded()))
        ) {
            throw new CorruptedFileException();
        }
        query = fsLessonsContent.getQuery();
        title = fsLessonsContent.getTitle();
        type = fsLessonsContent.getType();
        thread.runOnUI(() -> {
            TextView shareTitle = fragment.container().findViewById(R.id.share_title);
            if (shareTitle != null) {
                shareTitle.setText(scheduleLessons.getScheduleHeader(fsLessonsContent.getTitle(), fsLessonsContent.getType()));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            finish();
        });
        changes.clear();
        ArrayList<FSLAddedDay> added = fsLessonsContent.getAdded();
        if (CollectionUtils.isNotEmpty(added)) {
            for (FSLAddedDay day : added) {
                if (day.getLesson() == null) {
                    continue;
                }
                changes.add(new Change(ADDED, true, day.getDay(), day.getLesson()));
            }
        }
        ArrayList<FSLReducedDay> reduced = fsLessonsContent.getReduced();
        if (CollectionUtils.isEmpty(reduced)) {
            if (changes.size() == 0) {
                throw new MessageException(activity.getString(R.string.no_changes));
            }
            display();
            return;
        }
        scheduleLessons.search(query, false, false, new Schedule.Handler<SLessons>() {
            @Override
            public void onSuccess(final SLessons schedule, final boolean fromCache) {
                thread.run(() -> {
                    log.v(TAG, "loadShare | success | schedule=", schedule);
                    if (schedule == null || Objects.equals("teachers", schedule.getType())) {
                        notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                        finish();
                        return;
                    }
                    for (FSLReducedDay day : reduced) {
                        if (day.getLesson() == null || StringUtils.isBlank(day.getLesson().getHash())) {
                            continue;
                        }
                        SLesson lesson = getLessonByHash(schedule, day.getDay(), day.getLesson().getHash());
                        if (lesson != null) {
                            changes.add(new Change(REDUCED, true, day.getDay(), lesson));
                        }
                    }
                    display();
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    finish();
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "loadShare | failure ", state);
                    ViewGroup shareContent = fragment.container().findViewById(R.id.share_content);
                    if (shareContent == null) {
                        return;
                    }
                    shareContent.removeAllViews();
                    View view;
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case ScheduleLessons.FAILED_OFFLINE:
                            shareContent.addView(fragment.inflate(R.layout.state_offline_text_compact));
                            break;
                        case Client.FAILED_SERVER_ERROR:
                            view = fragment.inflate(R.layout.state_failed_text_compact);
                            ((TextView) view.findViewById(R.id.text)).setText(Client.getFailureMessage(activity, statusCode));
                            shareContent.addView(view);
                            break;
                        case Client.FAILED_CORRUPTED_JSON:
                            view = fragment.inflate(R.layout.state_failed_text_compact);
                            ((TextView) view.findViewById(R.id.text)).setText(R.string.server_provided_corrupted_json);
                            shareContent.addView(view);
                            break;
                        case Client.FAILED_TRY_AGAIN:
                        case ScheduleLessons.FAILED_LOAD:
                        case ScheduleLessons.FAILED_EMPTY_QUERY:
                        case ScheduleLessons.FAILED_NOT_FOUND:
                        case ScheduleLessons.FAILED_INVALID_QUERY:
                        case ScheduleLessons.FAILED_MINE_NEED_ISU:
                        default:
                            shareContent.addView(fragment.inflate(R.layout.state_failed_text_compact));
                            break;
                    }
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    finish();
                });
            }
            @Override
            public void onProgress(final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "loadShare | progress ", state);
                    ViewGroup shareContent = fragment.container().findViewById(R.id.share_content);
                    if (shareContent != null) {
                        shareContent.removeAllViews();
                        shareContent.addView(fragment.inflate(R.layout.state_loading_text_compact));
                    }
                }, throwable -> {
                    log.exception(throwable);
                });
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
        });
    }
    
    private void loadShare(final Bundle extras) throws Exception {
        query = extras.getString("query");
        title = extras.getString("title");
        type = extras.getString("type");
        if (StringUtils.isBlank(query) || StringUtils.isBlank(title) || StringUtils.isBlank(type)) {
            throw new NullPointerException("Some extras are blank: " + query + " | " + title + " | " + type);
        }
        thread.runOnUI(() -> {
            TextView shareTitle = fragment.container().findViewById(R.id.share_title);
            if (shareTitle != null) {
                shareTitle.setText(scheduleLessons.getScheduleHeader(title, type));
            }
            ViewGroup shareInfo = fragment.container().findViewById(R.id.share_info);
            if (shareInfo != null) {
                shareInfo.setOnClickListener(view -> thread.run(() -> {
                    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.share_changes)
                            .setMessage(R.string.share_changes_info)
                            .setPositiveButton(R.string.close, null)
                            .create().show();
                }));
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            finish();
        });
        String token = query.toLowerCase();
        changes.clear();
        String addedString = storage.get(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, null);
        if (StringUtils.isNotBlank(addedString)) {
            SLessonsAdded added = new SLessonsAdded().fromJsonString(addedString);
            if (added != null && CollectionUtils.isNotEmpty(added.getSchedule())) {
                for (SDay day : added.getSchedule()) {
                    if (CollectionUtils.isEmpty(day.getLessons())) {
                        continue;
                    }
                    for (SLesson lesson : day.getLessons()) {
                        changes.add(new Change(ADDED, true, day.getWeekday(), lesson));
                    }
                }
            }
        }
        String reducedString = storage.get(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, null);
        if (StringUtils.isBlank(reducedString)) {
            display();
            return;
        }
        SLessonsReduced reduced = new SLessonsReduced().fromJsonString(reducedString);
        if (reduced == null || CollectionUtils.isEmpty(reduced.getSchedule())) {
            display();
            return;
        }
        scheduleLessons.search(query, false, false, new Schedule.Handler<SLessons>() {
            @Override
            public void onSuccess(final SLessons schedule, final boolean fromCache) {
                thread.run(() -> {
                    log.v(TAG, "loadShare | success | schedule=", schedule);
                    if (schedule == null || Objects.equals("teachers", schedule.getType())) {
                        notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                        finish();
                        return;
                    }
                    for (SDayReduced day : reduced.getSchedule()) {
                        if (CollectionUtils.isEmpty(day.getLessons())) {
                            continue;
                        }
                        for (String hash : day.getLessons()) {
                            SLesson lesson = getLessonByHash(schedule, day.getWeekday(), hash);
                            if (lesson != null) {
                                changes.add(new Change(REDUCED, true, day.getWeekday(), lesson));
                            }
                        }
                    }
                    display();
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    finish();
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "loadShare | failure ", state);
                    ViewGroup shareContent = fragment.container().findViewById(R.id.share_content);
                    if (shareContent == null) {
                        return;
                    }
                    shareContent.removeAllViews();
                    View view;
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case ScheduleLessons.FAILED_OFFLINE:
                            shareContent.addView(fragment.inflate(R.layout.state_offline_text_compact));
                            break;
                        case Client.FAILED_SERVER_ERROR:
                            view = fragment.inflate(R.layout.state_failed_text_compact);
                            ((TextView) view.findViewById(R.id.text)).setText(Client.getFailureMessage(activity, statusCode));
                            shareContent.addView(view);
                            break;
                        case Client.FAILED_CORRUPTED_JSON:
                            view = fragment.inflate(R.layout.state_failed_text_compact);
                            ((TextView) view.findViewById(R.id.text)).setText(R.string.server_provided_corrupted_json);
                            shareContent.addView(view);
                            break;
                        case Client.FAILED_TRY_AGAIN:
                        case ScheduleLessons.FAILED_LOAD:
                        case ScheduleLessons.FAILED_EMPTY_QUERY:
                        case ScheduleLessons.FAILED_NOT_FOUND:
                        case ScheduleLessons.FAILED_INVALID_QUERY:
                        case ScheduleLessons.FAILED_MINE_NEED_ISU:
                        default:
                            shareContent.addView(fragment.inflate(R.layout.state_failed_text_compact));
                            break;
                    }
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
                    finish();
                });
            }
            @Override
            public void onProgress(final int state) {
                thread.runOnUI(() -> {
                    log.v(TAG, "loadShare | progress ", state);
                    ViewGroup shareContent = fragment.container().findViewById(R.id.share_content);
                    if (shareContent != null) {
                        shareContent.removeAllViews();
                        shareContent.addView(fragment.inflate(R.layout.state_loading_text_compact));
                    }
                }, throwable -> {
                    log.exception(throwable);
                });
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
        });
    }
    
    private void display() {
        thread.runOnUI(() -> {
            log.v(TAG, "display | action=", action);
            ViewGroup shareContent = fragment.container().findViewById(R.id.share_content);
            if (shareContent == null) {
                return;
            }
            shareContent.removeAllViews();
            if (changes.size() == 0) {
                ViewGroup nothingToDisplay = (ViewGroup) fragment.inflate(R.layout.state_nothing_to_display_compact);
                ((TextView) nothingToDisplay.findViewById(R.id.ntd_text)).setText(R.string.nothing_to_share);
                shareContent.addView(nothingToDisplay);
                return;
            }
            boolean headerAdded = true;
            boolean headerReduced = true;
            for (Change change : changes) {
                try {
                    if (headerAdded && change.type.equals(ADDED)) {
                        headerAdded = false;
                        View header = fragment.inflate(R.layout.layout_schedule_lessons_share_header);
                        ((TextView) header.findViewById(R.id.text)).setText("handle".equals(action) ? R.string.add_lessons : R.string.added_lessons);
                        shareContent.addView(header);
                    }
                    if (headerReduced && change.type.equals(REDUCED)) {
                        headerReduced = false;
                        View header = fragment.inflate(R.layout.layout_schedule_lessons_share_header);
                        ((TextView) header.findViewById(R.id.text)).setText("handle".equals(action) ? R.string.reduce_lessons : R.string.reduced_lessons);
                        shareContent.addView(header);
                    }
                    View itemView = fragment.inflate(R.layout.layout_schedule_lessons_share_item);
                    ViewGroup lessonView = (ViewGroup) fragment.inflate(R.layout.layout_schedule_lessons_item);
                    CheckBox checkboxView = itemView.findViewById(R.id.checkbox);
                    ViewGroup contentView = itemView.findViewById(R.id.content);
                    checkboxView.setChecked(change.enabled);
                    lessonView.findViewById(R.id.lesson_reduced_icon).setVisibility(View.GONE);
                    lessonView.findViewById(R.id.lesson_synthetic_icon).setVisibility(View.GONE);
                    lessonView.findViewById(R.id.lesson_touch_icon).setVisibility(View.GONE);
                    ((TextView) lessonView.findViewById(R.id.lesson_time_start)).setText(change.lesson.getTimeStart());
                    ((TextView) lessonView.findViewById(R.id.lesson_time_end)).setText(change.lesson.getTimeEnd());
                    ((TextView) lessonView.findViewById(R.id.lesson_title)).setText(change.lesson.getSubject());
                    setDesc(change.lesson, lessonView.findViewById(R.id.lesson_desc));
                    setFlags(change.lesson, lessonView.findViewById(R.id.lesson_flags));
                    setMeta(change.lesson, lessonView.findViewById(R.id.lesson_meta));
                    checkboxView.setOnCheckedChangeListener((compoundButton, checked) -> change.enabled = checked);
                    itemView.setOnClickListener(view -> checkboxView.setChecked(!checkboxView.isChecked()));
                    contentView.addView(lessonView);
                    shareContent.addView(itemView);
                } catch (Exception e) {
                    log.exception(e);
                }
            }
            switch (action) {
                case "share":
                default: {
                    Button shareExecute = fragment.container().findViewById(R.id.share_execute);
                    if (shareExecute != null) {
                        shareExecute.setVisibility(View.VISIBLE);
                        shareExecute.setOnClickListener(view -> execute());
                    }
                    break;
                }
                case "handle": {
                    Button actionDeny = fragment.container().findViewById(R.id.action_deny);
                    Button actionAccept = fragment.container().findViewById(R.id.action_accept);
                    if (actionDeny != null) {
                        actionDeny.setOnClickListener(view -> finish());
                    }
                    if (actionAccept != null) {
                        actionAccept.setVisibility(View.VISIBLE);
                        actionAccept.setOnClickListener(view -> execute());
                    }
                    break;
                }
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.toast(activity, activity.getString(R.string.something_went_wrong));
            finish();
        });
    }
    
    private void execute() {
        thread.run(() -> {
            log.v(TAG, "execute | action=", action);
            boolean selected = false;
            for (Change change : changes) {
                if (change.enabled) {
                    selected = true;
                    break;
                }
            }
            if (!selected) {
                notificationMessage.snackBar(activity, activity.getString(R.string.nothing_to_share));
                return;
            }
            switch (action) {
                case "handle": executeHandle(); break;
                case "share": default: executeShare(); break;
            }
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }
    
    private void executeHandle() throws Exception {
        String token = query.toLowerCase();
        for (Change change : changes) {
            if (!change.enabled) {
                continue;
            }
            if (ADDED.equals(change.type)) {
                change.lesson.setCdoitmoType("synthetic");
                boolean found = false;
                String addedString = storage.get(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, null);
                SLessonsAdded added = new SLessonsAdded();
                if (StringUtils.isNotBlank(addedString)) {
                    added.fromJsonString(addedString);
                } else {
                    added.setSchedule(new ArrayList<>());
                }
                added.setTimestamp(time.getTimeInMillis());
                for (SDay day : added.getSchedule()) {
                    if (day.getWeekday() == change.weekday) {
                        if (CollectionUtils.isEmpty(day.getLessons())) {
                            day.setLessons(new ArrayList<>());
                        }
                        day.getLessons().add(change.lesson);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    SDay day = new SDay();
                    day.setWeekday(change.weekday);
                    day.setLessons(new ArrayList<>());
                    day.getLessons().add(change.lesson);
                    added.getSchedule().add(day);
                }
                storage.put(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, added.toJsonString());
            }
            if (REDUCED.equals(change.type)) {
                boolean found = false;
                String hash = scheduleLessonsHelper.getLessonHash(change.lesson);
                String reducedString = storage.get(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, null);
                SLessonsReduced reduced = new SLessonsReduced();
                if (StringUtils.isNotBlank(reducedString)) {
                    reduced.fromJsonString(reducedString);
                } else {
                    reduced.setSchedule(new ArrayList<>());
                }
                for (SDayReduced day : reduced.getSchedule()) {
                    if (day.getWeekday() == change.weekday) {
                        if (CollectionUtils.isEmpty(day.getLessons())) {
                            day.setLessons(new ArrayList<>());
                        }
                        boolean lessonFound = false;
                        for (String lessonHash : day.getLessons()) {
                            if (Objects.equals(lessonHash, hash)) {
                                lessonFound = true;
                                break;
                            }
                        }
                        if (!lessonFound) {
                            day.getLessons().add(hash);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    SDayReduced day = new SDayReduced();
                    day.setWeekday(change.weekday);
                    day.setLessons(new ArrayList<>());
                    day.getLessons().add(hash);
                    reduced.getSchedule().add(day);
                }
                storage.put(activity, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, reduced.toJsonString());
            }
        }
        firebaseAnalyticsProvider.logEvent(
                activity,
                FirebaseAnalyticsProvider.Event.RECEIVE,
                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "schedule_lessons")
        );
        notificationMessage.toast(activity, fragment.getString(R.string.changes_applied));
        finish();
    }
    
    private void executeShare() throws Exception {
        FSLessonsContent content = new FSLessonsContent();
        content.setQuery(query);
        content.setType(type);
        content.setTitle(title);
        content.setAdded(new ArrayList<>());
        content.setReduced(new ArrayList<>());
        FSLessons data = new FSLessons();
        data.setType(TYPE);
        data.setVersion(VERSION);
        data.setContent(content);
        for (Change change : changes) {
            if (!change.enabled) {
                continue;
            }
            if (ADDED.equals(change.type)) {
                FSLAddedDay day = new FSLAddedDay();
                day.setDay(change.weekday);
                day.setLesson(change.lesson);
                content.getAdded().add(day);
            }
            if (REDUCED.equals(change.type)) {
                FSLReducedDay day = new FSLReducedDay();
                day.setDay(change.weekday);
                day.setLesson(new FSLReduced(scheduleLessonsHelper.getLessonHash(change.lesson)));
                content.getReduced().add(day);
            }
        }
        try {
            byte[] bytes = data.toJsonString().getBytes(StandardCharsets.UTF_8);
            File file = makeFile(bytes);
            Uri uri = FileProvider.getUriForFile(activity, "com.bukhmastov.cdoitmo.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("application/cdoitmo" /*activity.getContentResolver().getType(uri)*/ );
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.share) + "...");
            eventBus.fire(new OpenIntentEvent(chooserIntent));
            firebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "file_slessons_changes")
            );
        } catch (Exception ignore) {
            notificationMessage.toast(activity, activity.getString(R.string.failed_to_share_file));
        }
    }

    private void setDesc(SLesson lesson, TextView textView) {
        String desc;
        String teacher = "";
        String group = "";
        if (StringUtils.isNotBlank(lesson.getTeacherName())) {
            teacher = lesson.getTeacherName().trim();
        }
        if (StringUtils.isNotBlank(lesson.getGroup())) {
            group = lesson.getGroup().trim();
        }
        if (StringUtils.isBlank(group)) {
            desc = teacher;
        } else {
            desc = group;
            if (StringUtils.isNotBlank(teacher)) {
                desc += " (" + teacher + ")";
            }
        }
        if (StringUtils.isNotBlank(desc)) {
            textView.setText(desc);
        } else {
            textView.setHeight(0);
        }
    }
    
    private void setMeta(SLesson lesson, TextView textView) {
        String meta;
        String room = "";
        String building = "";
        if (StringUtils.isNotBlank(lesson.getRoom())) {
            room = lesson.getRoom().trim();
        }
        if (StringUtils.isNotBlank(lesson.getBuilding())) {
            building = lesson.getBuilding().trim();
        }
        if (StringUtils.isBlank(room)) {
            meta = building;
        } else {
            meta = activity.getString(R.string.room_short) + " " + room;
            if (StringUtils.isNotBlank(building)) {
                meta += " (" + building + ")";
            }
        }
        if (StringUtils.isNotBlank(meta)) {
            textView.setText(meta);
        } else {
            textView.setHeight(0);
        }
    }
    
    private void setFlags(SLesson lesson, ViewGroup viewGroup) {
        if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Color.resolve(activity, R.attr.colorScheduleFlagTEXT);
        if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Color.resolve(activity, R.attr.colorScheduleFlagCommonBG);
        if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Color.resolve(activity, R.attr.colorScheduleFlagPracticeBG);
        if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Color.resolve(activity, R.attr.colorScheduleFlagLectureBG);
        if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Color.resolve(activity, R.attr.colorScheduleFlagLabBG);
        if (colorScheduleFlagIwsBG == -1) colorScheduleFlagIwsBG = Color.resolve(activity, R.attr.colorScheduleFlagIwsBG);
        if (StringUtils.isNotBlank(lesson.getType())) {
            switch (lesson.getType()) {
                case "practice":
                    viewGroup.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG));
                    break;
                case "lecture":
                    viewGroup.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG));
                    break;
                case "lab":
                    viewGroup.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG));
                    break;
                case "iws":
                    viewGroup.addView(getFlag(activity.getString(R.string.iws), colorScheduleFlagTEXT, colorScheduleFlagIwsBG));
                    break;
                default:
                    viewGroup.addView(getFlag(lesson.getType(), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
                    break;
            }
        }
        if (lesson.getParity() == 0 || lesson.getParity() == 1) {
            viewGroup.addView(getFlag(lesson.getParity() == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
        }
    }
    
    private FrameLayout getFlag(String text, int textColor, int backgroundColor) {
        FrameLayout flagContainer = (FrameLayout) fragment.inflate(R.layout.layout_schedule_lessons_flag);
        TextView flag_content = flagContainer.findViewById(R.id.flag_content);
        flag_content.setText(text);
        flag_content.setBackgroundColor(backgroundColor);
        flag_content.setTextColor(textColor);
        return flagContainer;
    }
    
    private File makeFile(byte[] data) throws Exception {
        File file = new File(activity.getCacheDir(), "shared" + File.separator + "lessons_schedule_changes_" + ((int) time.getTimeInMillis() / 1000) + ".cdoitmo");
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs() && !file.createNewFile()) {
                throw new Exception("Failed to create file: " + file.getPath());
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(data);
        bos.flush();
        bos.close();
        fos.close();
        file.deleteOnExit();
        return file;
    }

    private @Nullable SLesson getLessonByHash(SLessons schedule, int weekday, String hash) {
        thread.assertNotUI();
        if (schedule == null || CollectionUtils.isEmpty(schedule.getSchedule())) {
            return null;
        }
        for (SDay day : schedule.getSchedule()) {
            if (day == null || day.getWeekday() != weekday || CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            for (SLesson lesson : day.getLessons()) {
                try {
                    if (Objects.equals(hash, scheduleLessonsHelper.getLessonHash(lesson))) {
                        return lesson;
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
        return null;
    }

    private void finish() {
        thread.runOnUI(() -> {
            log.v(TAG, "finish");
            if ("handle".equals(action)) {
                activity.finish();
            } else {
                activity.back();
            }
        });
    }
}
