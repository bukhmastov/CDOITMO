package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingListFragmentPresenter;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.parse.rating.RatingTopListParse;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;

import org.json.JSONArray;
import org.json.JSONObject;

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
        if (event.isNot("rating")) {
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
        log.v(TAG, "Fragment created");
        fragment.setHasOptionsMenu(true);
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
        fragment.clearData(fragment);
        hideShareButton();
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        if (!loaded) {
            loaded = true;
            try {
                String stored = fragment.restoreData(fragment);
                if (stored != null && !stored.isEmpty()) {
                    display(textUtils.string2json(stored));
                } else {
                    load();
                }
            } catch (Exception e) {
                log.exception(e);
                load();
            }
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onViewCreated() {
        activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
        try {
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
            log.v(TAG, "faculty=" + faculty + " | course=" + course + " | years=" + years);
            if (faculty == null || faculty.isEmpty() || course == null || course.isEmpty()) {
                throw new Exception("wrong extras provided | faculty=" + (faculty == null ? "<null>" : faculty) + " | course=" + (course == null ? "<null>" : course));
            }
        } catch (Exception e) {
            log.exception(e);
            activity.back();
        }
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
            if (!App.OFFLINE_MODE) {
                if (faculty == null || faculty.isEmpty() || course == null || course.isEmpty() || years == null || years.isEmpty()) {
                    log.w(TAG, "load | some data is empty | faculty=", faculty, " | course=", course, " | years=", years);
                    loadFailed();
                    return;
                }
                deIfmoClient.get(activity, "index.php?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        thread.run(() -> {
                            log.v(TAG, "load | success | statusCode=" + statusCode);
                            if (statusCode == 200) {
                                new RatingTopListParse(response, storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name"), json -> {
                                    if (json != null) {
                                        fragment.storeData(fragment, json.toString());
                                    }
                                    display(json);
                                }).run();
                            } else {
                                loadFailed();
                            }
                        });
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case DeIfmoClient.FAILED_OFFLINE:
                                    fragment.draw(R.layout.state_offline_text);
                                    View offline_reload = fragment.container().findViewById(R.id.offline_reload);
                                    if (offline_reload != null) {
                                        offline_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                                case DeIfmoClient.FAILED_TRY_AGAIN:
                                case DeIfmoClient.FAILED_SERVER_ERROR:
                                    fragment.draw(R.layout.state_failed_button);
                                    if (state == DeIfmoClient.FAILED_SERVER_ERROR) {
                                        TextView try_again_message = fragment.container().findViewById(R.id.try_again_message);
                                        if (try_again_message != null) {
                                            try_again_message.setText(DeIfmoClient.getFailureMessage(activity, statusCode));
                                        }
                                    }
                                    View try_again_reload = fragment.container().findViewById(R.id.try_again_reload);
                                    if (try_again_reload != null) {
                                        try_again_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        thread.runOnUI(() -> {
                            log.v(TAG, "load | progress " + state);
                            fragment.draw(R.layout.state_loading_text);
                            TextView loading_message = fragment.container().findViewById(R.id.loading_message);
                            if (loading_message != null) {
                                switch (state) {
                                    case DeIfmoClient.STATE_HANDLING:
                                        loading_message.setText(R.string.loading);
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
            } else {
                thread.runOnUI(() -> {
                    try {
                        notificationMessage.snackBar(activity, activity.getString(R.string.offline_mode_on));
                        fragment.draw(R.layout.state_offline_text);
                        View offline_reload = fragment.container().findViewById(R.id.offline_reload);
                        if (offline_reload != null) {
                            offline_reload.setOnClickListener(v -> load());
                        }
                    } catch (Exception e) {
                        log.exception(e);
                    }
                });
            }
        });
    }
    
    private void loadFailed() {
        thread.runOnUI(() -> {
            log.v(TAG, "loadFailed");
            try {
                fragment.draw(R.layout.state_failed_button);
                View try_again_reload = fragment.container().findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }
    
    private void display(final JSONObject data) {
        thread.run(() -> {
            log.v(TAG, "display");
            try {
                if (data == null) throw new SilentException();
                final String title = textUtils.capitalizeFirstLetter(data.getString("header"));
                activity.updateToolbar(activity, title, R.drawable.ic_rating);
                minePosition = -1;
                mineFaculty = "";
                hideShareButton();
                // получаем список для отображения рейтинга
                final JSONArray list = data.getJSONArray("list");
                final JSONArray rating = new JSONArray();
                if (list != null && list.length() > 0) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject jsonObject = list.getJSONObject(i);
                        if (jsonObject == null) continue;
                        int position = jsonObject.getInt("number");
                        boolean mine = jsonObject.getBoolean("is_me");
                        if (minePosition == -1 && mine) {
                            minePosition = position;
                            Matcher m = Pattern.compile("^(.*)\\(.*\\)$").matcher(title);
                            if (m.find()) {
                                mineFaculty = m.group(1).trim();
                            } else {
                                mineFaculty = title;
                            }
                        }
                        rating.put(new JSONObject()
                                .put("position", position)
                                .put("fio", jsonObject.getString("fio"))
                                .put("meta", jsonObject.getString("group") + " — " + jsonObject.getString("department"))
                                .put("mine", mine)
                                .put("change", jsonObject.getString("change"))
                                .put("delta", jsonObject.getString("delta"))
                        );
                    }
                }
                if (minePosition != -1) {
                    showShareButton();
                }
                final RatingListRVA adapter = new RatingListRVA(activity, rating);
                thread.runOnUI(() -> {
                    try {
                        fragment.draw(R.layout.layout_rating_list);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView rating_list = fragment.container().findViewById(R.id.rating_list);
                        if (rating_list != null) {
                            rating_list.setLayoutManager(layoutManager);
                            rating_list.setAdapter(adapter);
                            rating_list.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = fragment.container().findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                            swipe_container.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                            swipe_container.setOnRefreshListener(this);
                        }
                    } catch (Exception e) {
                        log.exception(e);
                        loadFailed();
                    }
                });
            } catch (SilentException ignore) {
                loadFailed();
            } catch (Exception e) {
                log.exception(e);
                loadFailed();
            }
        });
    }
    
    private void showShareButton() {
        thread.runOnUI(() -> {
            try {
                if (activity.toolbar != null) {
                    final MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                    if (action_share != null && minePosition != -1) {
                        action_share.setVisible(true);
                        action_share.setOnMenuItemClickListener(menuItem -> {
                            try {
                                share("Я на %position% позиции в рейтинге %faculty%!"
                                        .replace("%position%", String.valueOf(minePosition))
                                        .replace("%faculty%", mineFaculty.isEmpty() ? "факультета" : mineFaculty)
                                );
                            } catch (Exception e) {
                                log.exception(e);
                                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                            return false;
                        });
                    }
                }
            } catch (Exception e){
                log.exception(e);
            }
        });
    }
    
    private void hideShareButton() {
        thread.runOnUI(() -> {
            try {
                if (activity != null && activity.toolbar != null) {
                    MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                    if (action_share != null) action_share.setVisible(false);
                }
            } catch (Exception e){
                log.exception(e);
            }
        });
    }
    
    private void share(final String title) {
        thread.runOnUI(() -> {
            log.v(TAG, "share | " + title);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, title);
            eventBus.fire(new OpenIntentEvent(Intent.createChooser(intent, activity.getString(R.string.share))));
            // track statistics
            firebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "rating")
            );
        });
    }
}
