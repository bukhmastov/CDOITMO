package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.RatingListRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingListFragmentPresenter;
import com.bukhmastov.cdoitmo.model.parser.RatingTopListParser;
import com.bukhmastov.cdoitmo.model.rating.top.RStudent;
import com.bukhmastov.cdoitmo.model.rating.top.RatingTopList;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class RatingListFragmentPresenterImpl implements RatingListFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int minePosition = -1;
    private String mineFaculty = "";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    DeIfmoClient deIfmoClient;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public RatingListFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.RATING)) {
            return;
        }
        fragment.clearData(fragment);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(TAG, "Fragment created");
            fragment.setHasOptionsMenu(true);
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        });
    }

    @Override
    public void onDestroy() {
        thread.run(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            fragment.clearData(fragment);
            hideShareButton();
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (loaded) {
                return;
            }
            loaded = true;
            String stored = fragment.restoreData(fragment);
            if (StringUtils.isNotBlank(stored)) {
                display(new RatingTopList().fromJsonString(stored));
            } else {
                load();
            }
        }, throwable -> {
            log.exception(throwable);
            load();
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
    public void onViewCreated() {
        thread.runOnUI(() -> {
            activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                throw new NullPointerException("extras are null");
            }
            faculty = extras.getString("faculty");
            course = extras.getString("course");
            years = extras.getString("years");
            if (years == null || years.isEmpty()) {
                Calendar now = time.getCalendar();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH);
                years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
            }
            log.v(TAG, "faculty=", faculty, " | course=", course, " | years=", years);
            if (StringUtils.isBlank(faculty) || StringUtils.isBlank(course)) {
                throw new Exception("wrong extras provided | faculty=" + (faculty == null ? "<null>" : faculty) + " | course=" + (course == null ? "<null>" : course));
            }
        }, throwable -> {
            log.exception(throwable);
            activity.back();
        });
    }

    @Override
    public void onRefresh() {
        load();
    }

    private void load() {
        thread.run(() -> {
            log.v(TAG, "load");
            activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
            minePosition = -1;
            mineFaculty = "";
            hideShareButton();
            if (App.OFFLINE_MODE) {
                thread.runOnUI(() -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.offline_mode_on));
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                }, throwable -> {
                    log.exception(throwable);
                });
                return;
            }
            if (StringUtils.isBlank(faculty) || StringUtils.isBlank(course) || StringUtils.isBlank(years)) {
                log.w(TAG, "load | some data is empty | faculty=", faculty, " | course=", course, " | years=", years);
                loadFailed();
                return;
            }
            deIfmoClient.get(activity, "index.php?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new ResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=", statusCode);
                        if (statusCode == 200) {
                            RatingTopList ratingTopList = new RatingTopListParser(response, storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name")).parse();
                            if (ratingTopList != null) {
                                fragment.storeData(fragment, ratingTopList.toJsonString());
                            }
                            display(ratingTopList);
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | failure ", state);
                        switch (state) {
                            case DeIfmoClient.FAILED_OFFLINE: {
                                fragment.draw(R.layout.state_offline_text);
                                View reload = fragment.container().findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                            case DeIfmoClient.FAILED_TRY_AGAIN:
                            case DeIfmoClient.FAILED_SERVER_ERROR: {
                                fragment.draw(R.layout.state_failed_button);
                                if (state == DeIfmoClient.FAILED_SERVER_ERROR) {
                                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                                    if (message != null) {
                                        if (activity != null) {
                                            message.setText(DeIfmoClient.getFailureMessage(activity, statusCode));
                                        } else {
                                            message.setText(DeIfmoClient.getFailureMessage(statusCode));
                                        }
                                    }
                                }
                                View reload = fragment.container().findViewById(R.id.try_again_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                                break;
                            }
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(final int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case DeIfmoClient.STATE_HANDLING:
                                    message.setText(R.string.loading);
                                    break;
                            }
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }
    
    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            fragment.draw(R.layout.state_failed_button);
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }
    
    private void display(RatingTopList data) {
        thread.run(() -> {
            log.v(TAG, "display");
            if (data == null) {
                loadFailed();
                return;
            }
            String title = StringUtils.isNotBlank(data.getHeader()) ? textUtils.capitalizeFirstLetter(data.getHeader()) : "";
            activity.updateToolbar(activity, title, R.drawable.ic_rating);
            minePosition = -1;
            mineFaculty = "";
            hideShareButton();
            // получаем список для отображения рейтинга
            if (CollectionUtils.isNotEmpty(data.getStudents())) {
                for (RStudent student : data.getStudents()) {
                    if (student == null) {
                        continue;
                    }
                    if (minePosition == -1 && student.isMe()) {
                        minePosition = student.getNumber();
                        Matcher m = Pattern.compile("^(.*)\\(.*\\)$").matcher(title);
                        if (m.find()) {
                            mineFaculty = m.group(1).trim();
                        } else {
                            mineFaculty = title;
                        }
                        break;
                    }
                }
            }
            if (minePosition != -1) {
                showShareButton();
            }
            RatingListRVA adapter = new RatingListRVA(activity, data);
            thread.runOnUI(() -> {
                fragment.draw(R.layout.layout_rating_list);
                // set adapter to recycler view
                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                final RecyclerView recyclerView = fragment.container().findViewById(R.id.rating_list);
                if (recyclerView != null) {
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setHasFixedSize(true);
                }
                // setup swipe
                final SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.swipe_container);
                if (swipe != null) {
                    swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                    swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                    swipe.setOnRefreshListener(this);
                }
            }, throwable -> {
                log.exception(throwable);
                loadFailed();
            });
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }
    
    private void showShareButton() {
        thread.runOnUI(() -> {
            if (activity.toolbar != null) {
                MenuItem share = activity.toolbar.findItem(R.id.action_share);
                if (share != null && minePosition != -1) {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(menuItem -> {
                        try {
                            eventBus.fire(new ShareTextEvent("Я на %position% позиции в рейтинге %faculty%!"
                                    .replace("%position%", String.valueOf(minePosition))
                                    .replace("%faculty%", mineFaculty.isEmpty() ? "факультета" : mineFaculty)
                                    , "rating")
                            );
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                        return false;
                    });
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }
    
    private void hideShareButton() {
        thread.runOnUI(() -> {
            if (activity != null && activity.toolbar != null) {
                MenuItem share = activity.toolbar.findItem(R.id.action_share);
                if (share != null) {
                    share.setVisible(false);
                }
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }
}
