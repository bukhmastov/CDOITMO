package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.ArrayMap;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.LoginActivity;
import com.bukhmastov.cdoitmo.adapter.rva.RatingRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.RatingListFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingFragmentPresenter;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.parser.RatingPickerAllParser;
import com.bukhmastov.cdoitmo.model.parser.RatingPickerOwnParser;
import com.bukhmastov.cdoitmo.model.rating.pickerall.RatingPickerAll;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RCourse;
import com.bukhmastov.cdoitmo.model.rating.pickerown.RatingPickerOwn;
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

import java.util.ArrayList;

import javax.inject.Inject;

import static com.bukhmastov.cdoitmo.util.Thread.RA;

public class RatingFragmentPresenterImpl implements RatingFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RatingFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private final ArrayMap<String, Info> data = new ArrayMap<>();

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

    public RatingFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.RATING)) {
            return;
        }
        fragment.clearData();
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(RA);
        thread.run(RA, () -> {
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            data.put(COMMON, new Info(EMPTY));
            data.put(OWN, new Info(EMPTY));
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        thread.interrupt(RA);
        loaded = false;
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem share = menu.findItem(R.id.action_share);
            if (share != null) {
                if (data.get(OWN) == null || !(data.get(OWN).data instanceof RatingPickerOwn) || CollectionUtils.isEmpty(((RatingPickerOwn) data.get(OWN).data).getCourses())) {
                    share.setVisible(false);
                } else {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(item -> {
                        share();
                        return false;
                    });
                }
            }
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public void onResume() {
        thread.run(RA, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (loaded) {
                return;
            }
            loaded = true;
            String storedData = fragment.restoreData();
            String storedExtra = fragment.restoreDataExtra();
            RatingPickerAll storedAll = StringUtils.isNotBlank(storedData) ? new RatingPickerAll().fromJsonString(storedData) : null;
            RatingPickerOwn storedOwn = StringUtils.isNotBlank(storedExtra) ? new RatingPickerOwn().fromJsonString(storedExtra) : null;
            if (storedAll != null) {
                data.put(COMMON, new Info<>(LOADED, storedAll));
            }
            if (storedOwn != null) {
                data.put(OWN, new Info<>(LOADED, storedOwn));
            }
            if (storedAll == null || storedOwn == null) {
                load();
            } else {
                display();
            }
        }, throwable -> {
            log.exception(throwable);
            load();
        });
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
    public void onRefresh() {
        thread.run(RA, () -> {
            log.v(TAG, "refreshing");
            load(COMMON, true);
        });
    }

    private void load() {
        thread.run(RA, () -> load(COMMON));
    }

    private void load(@TYPE String type) {
        thread.run(RA, () -> {
            switch (type) {
                case COMMON: {
                    load(type, storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168")) : 0);
                    break;
                }
                case OWN: {
                    load(type, storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0);
                    break;
                }
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void load(@TYPE String type, int refresh_rate) {
        thread.run(RA, () -> {
            log.v(TAG, "load | type=", type, " | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(type, false);
                return;
            }
            JsonEntity cached = getFromCache(type);
            if (cached == null) {
                load(type, false);
                return;
            }
            data.get(type).data = COMMON.equals(type) ? (RatingPickerAll) cached : (RatingPickerOwn) cached;
            long timestamp;
            if (COMMON.equals(type)) {
                //noinspection ConstantConditions
                timestamp = ((RatingPickerAll) cached).getTimestamp();
            } else {
                //noinspection ConstantConditions
                timestamp = ((RatingPickerOwn) cached).getTimestamp();
            }
            if (timestamp + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(type, true, cached);
            } else {
                load(type, false, cached);
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void load(@TYPE String type, boolean force) {
        thread.run(RA, () -> load(type, force, null));
    }

    private <T extends JsonEntity> void load(@TYPE String type, boolean force, T cached) {
        thread.runOnUI(RA, () -> {
            fragment.draw(R.layout.state_loading_text);
            TextView message = fragment.container().findViewById(R.id.loading_message);
            if (message != null) {
                message.setText(R.string.loading);
            }
        });
        thread.run(RA, () -> {
            if (App.UNAUTHORIZED_MODE && OWN.equals(type)) {
                loaded(type);
                return;
            }
            log.v(TAG, "load | type=", type, " | force=", force);
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    JsonEntity cache = cached == null ? getFromCache(type) : cached;
                    if (cache != null) {
                        log.v(TAG, "load | type=", type, " | from cache");
                        data.put(type, new Info<>(LOADED, COMMON.equals(type) ? (RatingPickerAll) cache : (RatingPickerOwn) cache));
                        loaded(type);
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | type=", type, " | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (data.get(type).data != null) {
                    data.get(type).status = LOADED;
                    loaded(type);
                } else {
                    data.put(type, new Info(OFFLINE));
                    loaded(type);
                }
            }
            String url = COMMON.equals(type) ? "index.php?node=rating" : "servlet/distributedCDE?Rule=REP_EXECUTE_PRINT&REP_ID=1441";
            deIfmoClient.get(activity, url, null, new ResponseHandler() {
                @Override
                public void onSuccess(int code, Client.Headers headers, String response) throws Exception {
                    log.v(TAG, "load | type=", type, " | success | code=", code, " | response=", (response == null ? "null" : "notnull"));
                    if (code == 200 && response != null) {
                        switch (type) {
                            case COMMON: {
                                RatingPickerAll ratingPickerAll = new RatingPickerAllParser(response).parse();
                                if (ratingPickerAll != null) {
                                    ratingPickerAll.setTimestamp(time.getTimeInMillis());
                                    if (storagePref.get(activity, "pref_use_cache", true)) {
                                        storage.put(activity, Storage.CACHE, Storage.USER, "rating#list", ratingPickerAll.toJsonString());
                                    }
                                    data.put(type, new Info<>(LOADED, ratingPickerAll));
                                    loaded(type);
                                    return;
                                }
                                break;
                            }
                            case OWN: {
                                RatingPickerOwn ratingPickerOwn = new RatingPickerOwnParser(response).parse();
                                if (ratingPickerOwn != null) {
                                    ratingPickerOwn.setTimestamp(time.getTimeInMillis());
                                    if (storagePref.get(activity, "pref_use_cache", true)) {
                                        storage.put(activity, Storage.CACHE, Storage.USER, "rating#core", ratingPickerOwn.toJsonString());
                                    }
                                    data.put(type, new Info<>(LOADED, ratingPickerOwn));
                                    loaded(type);
                                    return;
                                }
                                break;
                            }
                        }
                    }
                    if (data.get(type).data != null) {
                        data.get(type).status = LOADED;
                    } else {
                        data.put(type, new Info(FAILED));
                    }
                    loaded(type);
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.run(RA, () -> {
                        log.v(TAG, "load | type=", type, " | failure ", state);
                        switch (state) {
                            case Client.FAILED_AUTH_CREDENTIALS_REQUIRED: {
                                loaded = false;
                                gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_REQUIRED);
                                break;
                            }
                            case Client.FAILED_AUTH_CREDENTIALS_FAILED: {
                                loaded = false;
                                gotoLogin(LoginActivity.SIGNAL_CREDENTIALS_FAILED);
                                break;
                            }
                            case Client.FAILED_ERROR_4XX:
                            case Client.FAILED_ERROR_5XX: {
                                data.put(type, new Info(SERVER_ERROR));
                                loaded(type);
                                break;
                            }
                            default: {
                                if (data.get(type).data != null) {
                                    data.get(type).status = LOADED;
                                    loaded(type);
                                } else {
                                    data.put(type, new Info(FAILED));
                                    loaded(type);
                                }
                                break;
                            }
                        }
                    });
                }
                @Override
                public void onProgress(int state) {
                    log.v(TAG, "load | type=", type, " | progress ", state);
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

    private void loaded(@TYPE String type) {
        thread.run(RA, () -> {
            switch (type) {
                case COMMON: {
                    if (App.UNAUTHORIZED_MODE) {
                        display();
                        break;
                    }
                    load(OWN);
                    break;
                }
                case OWN: {
                    display();
                    break;
                }
            }
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(RA, () -> {
            log.v(TAG, "loadFailed");
            fragment.draw(R.layout.state_failed_button);
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {});
    }

    private void display() {
        thread.run(RA, () -> {
            log.v(TAG, "display");
            fragment.storeData(
                    data.containsKey(COMMON) ? (data.get(COMMON).data != null ? data.get(COMMON).data.toJsonString() : null) : null,
                    data.containsKey(OWN) ? (data.get(OWN).data != null ? data.get(OWN).data.toJsonString() : null) : null
            );
            RatingRVA adapter = new RatingRVA(activity, data);
            adapter.setClickListener(R.id.common_apply, (v, data) -> {
                thread.runOnUI(RA, () -> {
                    firebaseAnalyticsProvider.logBasicEvent(activity, "Detailed rating used");
                    log.v(TAG, "detailed rating used | faculty=" + data.getTitle() + " | course=" + data.getDesc());
                    Bundle extras = new Bundle();
                    extras.putString("faculty", data.getTitle());
                    extras.putString("course", data.getDesc());
                    activity.openActivityOrFragment(RatingListFragment.class, extras);
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            adapter.setClickListener(R.id.own_apply, (v, data) -> {
                thread.runOnUI(RA, () -> {
                    firebaseAnalyticsProvider.logBasicEvent(activity, "Own rating used");
                    log.v(TAG, "own rating used | faculty=" + data.getDesc() + " | course=" + data.getMeta() + " | years=" + data.getExtra());
                    Bundle extras = new Bundle();
                    extras.putString("faculty", data.getDesc());
                    extras.putString("course", data.getMeta());
                    extras.putString("years", data.getExtra());
                    activity.openActivityOrFragment(RatingListFragment.class, extras);
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            adapter.setClickListener(R.id.faculty, (v, data) -> {
                thread.run(RA, () -> {
                    storage.put(activity, Storage.CACHE, Storage.USER, "rating#choose#faculty", data.getExtra());
                }, throwable -> {
                    log.exception(throwable);
                });
            });
            adapter.setClickListener(R.id.course, (v, data) -> {
                thread.run(RA, () -> {
                    storage.put(activity, Storage.CACHE, Storage.USER, "rating#choose#course", data.getExtra());
                }, throwable -> {
                    log.exception(throwable);
                });
            });
            thread.runOnUI(RA, () -> {
                onToolbarSetup(fragment.toolbar());
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

    private void share() {
        thread.run(RA, () -> {
            if (data.get(OWN) == null || !(data.get(OWN).data instanceof RatingPickerOwn) || CollectionUtils.isEmpty(((RatingPickerOwn) data.get(OWN).data).getCourses())) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Мой рейтинг:").append("\n");
            ArrayList<RCourse> courses = ((RatingPickerOwn) data.get(OWN).data).getCourses();
            for (RCourse course : courses) {
                sb.append(course.getFaculty());
                sb.append(" ");
                sb.append(String.valueOf(course.getCourse()));
                sb.append(" ");
                sb.append(activity.getString(R.string.course));
                sb.append(" — ");
                sb.append(course.getPosition());
                sb.append("\n");
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_rating_all"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private JsonEntity getFromCache(@TYPE String type) {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.USER, COMMON.equals(type) ? "rating#list" : "rating#core").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            if (COMMON.equals(type)) {
                return new RatingPickerAll().fromJsonString(cache);
            } else {
                return new RatingPickerOwn().fromJsonString(cache);
            }
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.USER, COMMON.equals(type) ? "rating#list" : "rating#core");
            return null;
        }
    }

    private void gotoLogin(int state) {
        thread.run(RA, () -> {
            log.v(TAG, "gotoLogin | state=", state);
            Bundle extras = new Bundle();
            extras.putInt("state", state);
            eventBus.fire(new OpenActivityEvent(LoginActivity.class, extras));
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }
}
