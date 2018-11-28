package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.GroupRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuGroupInfoFragmentPresenter;
import com.bukhmastov.cdoitmo.model.group.GList;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class IsuGroupInfoFragmentPresenterImpl implements IsuGroupInfoFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuGroupInfoFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private GList data = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;

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
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public IsuGroupInfoFragmentPresenterImpl() {
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
            log.v(TAG, "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                if (getData() == null) {
                    load();
                } else {
                    display();
                }
            }
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
    public void onRefresh() {
        thread.run(() -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_static_refresh", "168")) : 0));
    }

    private void load(int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            GList cache = getFromCache();
            if (cache == null) {
                load(true, null);
                return;
            }
            setData(cache);
            if (cache.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        });
    }

    private void load(boolean force) {
        thread.run(() -> load(force, null));
    }

    private void load(boolean force, GList cached) {
        thread.run(() -> {
            log.v(TAG, "load | force=", force);
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    GList cache = cached == null ? getFromCache() : cached;
                    if (cache != null) {
                        log.v(TAG, "load | from cache");
                        setData(cache);
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (getData() != null) {
                    display();
                    return;
                }
                thread.runOnUI(() -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (StringUtils.isBlank(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""))) {
                fragment.draw(R.layout.layout_isu_required);
                View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                if (openIsuAuth != null) {
                    openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                }
                return;
            }
            isuPrivateRestClient.get(activity, "groups/user/%apikey%/%isutoken%", null, new RestResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, JSONObject obj, JSONArray arr) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=", statusCode, " | obj=", obj);
                        if (statusCode == 200 && obj != null) {
                            GList data = new GList().fromJson(obj);
                            data.setTimestamp(time.getTimeInMillis());
                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                storage.put(activity, Storage.CACHE, Storage.USER, "group#core", data.toJsonString());
                            }
                            setData(data);
                            display();
                            return;
                        }
                        if (getData() != null) {
                            display();
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    thread.run(() -> {
                        log.v(TAG, "load | failure ", state);
                        switch (state) {
                            case IsuPrivateRestClient.FAILED_OFFLINE:
                                if (getData() != null) {
                                    display();
                                    return;
                                }
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_offline_text);
                                    View reload = fragment.container().findViewById(R.id.offline_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load());
                                    }
                                }, throwable -> {
                                    loadFailed();
                                });
                                break;
                            case IsuPrivateRestClient.FAILED_TRY_AGAIN:
                            case IsuPrivateRestClient.FAILED_SERVER_ERROR:
                            case IsuPrivateRestClient.FAILED_CORRUPTED_JSON:
                            case IsuPrivateRestClient.FAILED_AUTH_TRY_AGAIN:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_failed_button);
                                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                                    if (message != null) {
                                        switch (state) {
                                            case IsuPrivateRestClient.FAILED_SERVER_ERROR:
                                                if (activity == null) {
                                                    message.setText(IsuPrivateRestClient.getFailureMessage());
                                                } else {
                                                    message.setText(IsuPrivateRestClient.getFailureMessage(activity, statusCode));
                                                }
                                                break;
                                            case IsuPrivateRestClient.FAILED_CORRUPTED_JSON:
                                                message.setText(R.string.server_provided_corrupted_json);
                                                break;
                                        }
                                    }
                                    View reload = fragment.container().findViewById(R.id.try_again_reload);
                                    if (reload != null) {
                                        reload.setOnClickListener(v -> load());
                                    }
                                }, throwable -> {
                                    loadFailed();
                                });
                                break;
                            case IsuPrivateRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                            case IsuPrivateRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.layout_isu_required);
                                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                                    if (openIsuAuth != null) {
                                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                                    }
                                }, throwable -> {
                                    loadFailed();
                                });
                                break;
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(() -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            switch (state) {
                                case IsuPrivateRestClient.STATE_HANDLING: message.setText(R.string.loading); break;
                                case IsuPrivateRestClient.STATE_CHECKING: message.setText(R.string.auth_check); break;
                                case IsuPrivateRestClient.STATE_AUTHORIZATION: message.setText(R.string.authorization); break;
                                case IsuPrivateRestClient.STATE_AUTHORIZED: message.setText(R.string.authorized); break;
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
            TextView message = fragment.container().findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed);
            }
            View reload = fragment.container().findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {});
    }

    private void display() {
        thread.run(() -> {
            log.v(TAG, "display");
            GList data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            GroupRVA adapter = new GroupRVA(activity, data);
            adapter.setClickListener(R.id.person, (v, person) -> {
                thread.run(() -> {
                    if (person == null || StringUtils.isBlank(person.getUrl())) {
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(person.getUrl()));
                    eventBus.fire(new OpenIntentEvent(intent));
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            thread.runOnUI(() -> {
                onToolbarSetup(activity.toolbar);
                fragment.draw(R.layout.layout_rva);
                // set adapter to recycler view
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
                RecyclerView recyclerView = fragment.container().findViewById(R.id.recyclerView);
                if (recyclerView != null) {
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setHasFixedSize(true);
                }
                // setup swipe
                SwipeRefreshLayout swipe = fragment.container().findViewById(R.id.swipe_container);
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

    private GList getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.USER, "group#core").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new GList().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.USER, "group#core");
            return null;
        }
    }

    private void setData(GList data) {
        thread.assertNotUI();
        try {
            this.data = data;
            fragment.storeData(fragment, data.toJsonString());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private GList getData() {
        thread.assertNotUI();
        if (data != null) {
            return data;
        }
        try {
            String stored = fragment.restoreData(fragment);
            if (stored != null && !stored.isEmpty()) {
                data = new GList().fromJsonString(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}
