package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
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
import com.bukhmastov.cdoitmo.util.StoragePref;
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

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.RAL;

public class RatingListFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<RatingTopList>
        implements RatingListFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingListFragment";
    private String faculty = null;
    private String course = null;
    private String years = null;
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
    StoragePref storagePref;
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
        super(RatingTopList.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.RATING)) {
            return;
        }
        clearData();
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(RAL, () -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            fragment.setHasOptionsMenu(true);
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
        fragment.clearData();
        thread.interrupt(RAL);
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        if (menu == null) {
            return;
        }
        showShareButton(menu);
        MenuItem beer = menu.findItem(R.id.action_beer);
        MenuItem crown = menu.findItem(R.id.action_crown);
        if (beer != null && crown != null) {
            switch (storagePref.get(activity, "pref_rating_list_icon", "crown")) {
                case "crown": beer.setVisible(true); break;
                case "beer": crown.setVisible(true); break;
            }
            crown.setOnMenuItemClickListener(item -> {
                thread.runOnUI(RAL, () -> {
                    storagePref.put(activity, "pref_rating_list_icon", "crown");
                    crown.setVisible(false);
                    beer.setVisible(true);
                    load();
                });
                return false;
            });
            beer.setOnMenuItemClickListener(item -> {
                thread.runOnUI(RAL, () -> {
                    storagePref.put(activity, "pref_rating_list_icon", "beer");
                    beer.setVisible(false);
                    crown.setVisible(true);
                    load();
                });
                return false;
            });
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
        thread.standalone(() -> {
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onViewCreated() {
        thread.runOnUI(RAL, () -> {
            activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
            Bundle extras = fragment.getArguments();
            if (extras == null) {
                log.e(TAG, "extras are null");
                loadFailed();
                return;
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
                log.e(TAG, "wrong extras provided | faculty=", faculty, " | course=", course);
                loadFailed();
                return;
            }
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    @Override
    public void onRefresh() {
        load();
    }

    protected void load() {
        thread.run(RAL, () -> {
            log.v(TAG, "load");
            activity.updateToolbar(activity, activity.getString(R.string.top_rating), R.drawable.ic_rating);
            minePosition = -1;
            mineFaculty = "";
            thread.runOnUI(RAL, () -> hideShareButton(fragment.toolbar()));
            if (App.OFFLINE_MODE) {
                thread.runOnUI(RAL, () -> {
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
            String url = String.format("index.php?node=rating&std&depId=%s&year=%s&app=%s", faculty, course, years);
            deIfmoClient.get(activity, url, null, new ResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, String response) {
                    log.v(TAG, "load | success | code=", code);
                    if (code == 200) {
                        RatingTopList ratingTopList = new RatingTopListParser(response, storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name")).parse();
                        setData(ratingTopList);
                        display(ratingTopList);
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.runOnUI(RAL, () -> {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            fragment.draw(R.layout.state_offline_text);
                            View reload = fragment.container().findViewById(R.id.offline_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                            return;
                        }
                        fragment.draw(R.layout.state_failed_button);
                        TextView message = fragment.container().findViewById(R.id.try_again_message);
                        if (message != null) {
                            message.setText(deIfmoClient.getFailedMessage(activity, code, state));
                        }
                        View reload = fragment.container().findViewById(R.id.try_again_reload);
                        if (reload != null) {
                            reload.setOnClickListener(v -> load());
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(RAL, () -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(deIfmoClient.getProgressMessage(activity, state));
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
        thread.runOnUI(RAL, () -> {
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

    @Override
    protected void display() {
        display(getData());
    }

    private void display(RatingTopList data) {
        thread.run(RAL, () -> {
            log.v(TAG, "display");
            if (data == null) {
                loadFailed();
                return;
            }
            String title = StringUtils.isNotBlank(data.getHeader()) ? textUtils.capitalizeFirstLetter(data.getHeader()) : "";
            activity.updateToolbar(activity, title, R.drawable.ic_rating);
            minePosition = -1;
            mineFaculty = "";
            thread.runOnUI(RAL, () -> hideShareButton(fragment.toolbar()));
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
                thread.runOnUI(RAL, () -> showShareButton(fragment.toolbar()));
            }
            RatingListRVA adapter = new RatingListRVA(activity, data, "beer".equals(storagePref.get(activity, "pref_rating_list_icon", "crown")));
            thread.runOnUI(RAL, () -> {
                fragment.draw(R.layout.layout_rating_list);
                // set adapter to recycler view
                final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
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

    private void showShareButton(Menu menu) {
        if (menu == null) {
            return;
        }
        MenuItem share = menu.findItem(R.id.action_share);
        if (share == null) {
            return;
        }
        if (minePosition == -1) {
            share.setVisible(false);
            return;
        }
        share.setVisible(true);
        share.setOnMenuItemClickListener(menuItem -> {
            thread.run(RAL, () -> {
                eventBus.fire(new ShareTextEvent("Я на %position% позиции в рейтинге %faculty%!"
                        .replace("%position%", String.valueOf(minePosition))
                        .replace("%faculty%", mineFaculty.isEmpty() ? "факультета" : mineFaculty)
                        , "txt_rating_certain")
                );
            }, throwable -> {
                log.exception(throwable);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            });
            return false;
        });
    }

    private void hideShareButton(Menu menu) {
        if (menu == null) {
            return;
        }
        MenuItem share = menu.findItem(R.id.action_share);
        if (share != null) {
            share.setVisible(false);
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCacheType() {
        return null;
    }

    @Override
    protected String getCachePath() {
        return null;
    }

    @Override
    protected String getThreadToken() {
        return RAL;
    }
}
