package com.bukhmastov.cdoitmo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.RatingListRVA;
import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.parse.rating.RatingTopListParse;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Color;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingListFragment extends ConnectedFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListFragment";
    private String faculty = null;
    private String course = null;
    private String years = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private int minePosition = -1;
    private String mineFaculty = "";

    //@Inject
    private Storage storage = Storage.instance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideShareButton();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
        try {
            Bundle extras = getArguments();
            if (extras == null) {
                throw new NullPointerException("extras are null");
            }
            faculty = extras.getString("faculty");
            course = extras.getString("course");
            years = extras.getString("years");
            if (years == null || years.isEmpty()) {
                Calendar now = Time.getCalendar();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH);
                years = month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year;
            }
            Log.v(TAG, "faculty=" + faculty + " | course=" + course + " | years=" + years);
            if (faculty == null || faculty.isEmpty() || course == null || course.isEmpty()) {
                throw new Exception("wrong extras provided | faculty=" + Log.lString(faculty) + " | course=" + Log.lString(course));
            }
        } catch (Exception e) {
            Log.exception(e);
            activity.back();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "Fragment resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        if (!loaded) {
            loaded = true;
            try {
                String stored = restoreData(this);
                if (stored != null && !stored.isEmpty()) {
                    display(TextUtils.string2json(stored));
                } else {
                    load();
                }
            } catch (Exception e) {
                Log.exception(e);
                load();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "Fragment paused");
        if (requestHandle != null && requestHandle.cancel()) {
            loaded = false;
        }
    }

    @Override
    public void onRefresh() {
        load();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }

    private void load() {
        Thread.run(() -> {
            Log.v(TAG, "load");
            activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
            minePosition = -1;
            mineFaculty = "";
            hideShareButton();
            if (!App.OFFLINE_MODE) {
                if (faculty == null || faculty.isEmpty() || course == null || course.isEmpty() || years == null || years.isEmpty()) {
                    Log.w(TAG, "load | some data is empty | faculty=", faculty, " | course=", course, " | years=", years);
                    loadFailed();
                    return;
                }
                DeIfmoClient.get(activity, "index.php?node=rating&std&depId=" + faculty + "&year=" + course + "&app=" + years, null, new ResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Client.Headers headers, final String response) {
                        Thread.run(() -> {
                            Log.v(TAG, "load | success | statusCode=" + statusCode);
                            if (statusCode == 200) {
                                new RatingTopListParse(response, storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name"), json -> {
                                    if (json != null) {
                                        storeData(RatingListFragment.this, json.toString());
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
                        Thread.runOnUI(() -> {
                            Log.v(TAG, "load | failure " + state);
                            switch (state) {
                                case DeIfmoClient.FAILED_OFFLINE:
                                    draw(R.layout.state_offline_text);
                                    View offline_reload = container.findViewById(R.id.offline_reload);
                                    if (offline_reload != null) {
                                        offline_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                                case DeIfmoClient.FAILED_TRY_AGAIN:
                                case DeIfmoClient.FAILED_SERVER_ERROR:
                                    draw(R.layout.state_failed_button);
                                    if (state == DeIfmoClient.FAILED_SERVER_ERROR) {
                                        TextView try_again_message = container.findViewById(R.id.try_again_message);
                                        if (try_again_message != null) {
                                            try_again_message.setText(DeIfmoClient.getFailureMessage(activity, statusCode));
                                        }
                                    }
                                    View try_again_reload = container.findViewById(R.id.try_again_reload);
                                    if (try_again_reload != null) {
                                        try_again_reload.setOnClickListener(v -> load());
                                    }
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onProgress(final int state) {
                        Thread.runOnUI(() -> {
                            Log.v(TAG, "load | progress " + state);
                            draw(R.layout.state_loading_text);
                            TextView loading_message = container.findViewById(R.id.loading_message);
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
                Thread.runOnUI(() -> {
                    try {
                        BottomBar.snackBar(activity, activity.getString(R.string.offline_mode_on));
                        draw(R.layout.state_offline_text);
                        View offline_reload = container.findViewById(R.id.offline_reload);
                        if (offline_reload != null) {
                            offline_reload.setOnClickListener(v -> load());
                        }
                    } catch (Exception e) {
                        Log.exception(e);
                    }
                });
            }
        });
    }
    private void loadFailed() {
        Thread.runOnUI(() -> {
            Log.v(TAG, "loadFailed");
            try {
                draw(R.layout.state_failed_button);
                View try_again_reload = container.findViewById(R.id.try_again_reload);
                if (try_again_reload != null) {
                    try_again_reload.setOnClickListener(v -> load());
                }
            } catch (Exception e) {
                Log.exception(e);
            }
        });
    }
    private void display(final JSONObject data) {
        Thread.run(() -> {
            Log.v(TAG, "display");
            try {
                if (data == null) throw new SilentException();
                final String title = TextUtils.capitalizeFirstLetter(data.getString("header"));
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
                Thread.runOnUI(() -> {
                    try {
                        draw(R.layout.layout_rating_list);
                        // set adapter to recycler view
                        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                        final RecyclerView rating_list = container.findViewById(R.id.rating_list);
                        if (rating_list != null) {
                            rating_list.setLayoutManager(layoutManager);
                            rating_list.setAdapter(adapter);
                            rating_list.setHasFixedSize(true);
                        }
                        // setup swipe
                        final SwipeRefreshLayout swipe_container = container.findViewById(R.id.swipe_container);
                        if (swipe_container != null) {
                            swipe_container.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                            swipe_container.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                            swipe_container.setOnRefreshListener(this);
                        }
                    } catch (Exception e) {
                        Log.exception(e);
                        loadFailed();
                    }
                });
            } catch (SilentException ignore) {
                loadFailed();
            } catch (Exception e) {
                Log.exception(e);
                loadFailed();
            }
        });
    }
    private void showShareButton() {
        Thread.runOnUI(() -> {
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
                                Log.exception(e);
                                BottomBar.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                            return false;
                        });
                    }
                }
            } catch (Exception e){
                Log.exception(e);
            }
        });
    }
    private void hideShareButton() {
        Thread.runOnUI(() -> {
            try {
                if (activity != null && activity.toolbar != null) {
                    MenuItem action_share = activity.toolbar.findItem(R.id.action_share);
                    if (action_share != null) action_share.setVisible(false);
                }
            } catch (Exception e){
                Log.exception(e);
            }
        });
    }
    private void share(final String title) throws Exception {
        Thread.runOnUI(() -> {
            Log.v(TAG, "share | " + title);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, title);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share)));
            // track statistics
            FirebaseAnalyticsProvider.logEvent(
                    activity,
                    FirebaseAnalyticsProvider.Event.SHARE,
                    FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.TYPE, "rating")
            );
        });
    }
}
