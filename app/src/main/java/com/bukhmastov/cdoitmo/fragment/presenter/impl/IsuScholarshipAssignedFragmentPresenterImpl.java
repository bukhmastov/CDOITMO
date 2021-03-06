package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScholarshipAssignedRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuScholarshipAssignedFragmentPresenter;
import com.bukhmastov.cdoitmo.model.scholarship.assigned.SSAssigned;
import com.bukhmastov.cdoitmo.model.scholarship.assigned.SSAssignedList;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.ISA;

public class IsuScholarshipAssignedFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<SSAssignedList>
        implements IsuScholarshipAssignedFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuScholarshipAssignedFragment";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    EventBus eventBus;
    @Inject
    IsuPrivateRestClient isuPrivateRestClient;
    @Inject
    Time time;
    @Inject
    NotificationMessage notificationMessage;

    public IsuScholarshipAssignedFragmentPresenterImpl() {
        super(SSAssignedList.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.SCHOLARSHIP)) {
            return;
        }
        clearData();
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem share = menu.findItem(R.id.action_share);
            thread.run(ISA, () -> {
                if (share == null) {
                    return;
                }
                SSAssignedList data = getData();
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    thread.runOnUI(ISA, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(ISA, () -> {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(menuItem -> {
                        share();
                        return true;
                    });
                });
            });
        } catch (Throwable throwable) {
            log.exception(throwable);
        }
    }

    @Override
    public void onRefresh() {
        thread.run(ISA, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    protected void load() {
        thread.run(ISA, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) :
                    0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(ISA, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            SSAssignedList cache = getFromCache();
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
        thread.run(ISA, () -> load(force, null));
    }

    private void load(boolean force, SSAssignedList cached) {
        thread.run(ISA, () -> {
            log.v(TAG, "load | force=", force);
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    SSAssignedList cache = cached == null ? getFromCache() : cached;
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
                thread.runOnUI(ISA, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (StringUtils.isBlank(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""))) {
                thread.runOnUI(ISA, () -> {
                    fragment.draw(R.layout.layout_isu_required);
                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                    if (openIsuAuth != null) {
                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    }
                });
                return;
            }
            isuPrivateRestClient.get(activity, "scholarship/active/%apikey%/%isutoken%", null, new RestResponseHandler<SSAssignedList>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, SSAssignedList response) throws Exception {
                    log.v(TAG, "load | success | code=", code, " | response=", response);
                    if (code == 200 && response != null) {
                        response.setTimestamp(time.getTimeInMillis());
                        putToCache(response);
                        setData(response);
                        display();
                        return;
                    }
                    if (getData() != null) {
                        display();
                        return;
                    }
                    loadFailed();
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.run(ISA, () -> {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            thread.runOnUI(ISA, () -> {
                                fragment.draw(R.layout.state_offline_text);
                                View reload = fragment.container().findViewById(R.id.offline_reload);
                                if (reload != null) {
                                    reload.setOnClickListener(v -> load());
                                }
                            }, throwable -> {
                                loadFailed();
                            });
                            return;
                        }
                        if (isuPrivateRestClient.isFailedAuth(state)) {
                            thread.runOnUI(ISA, () -> {
                                fragment.draw(R.layout.layout_isu_required);
                                View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                                if (openIsuAuth != null) {
                                    openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                                }
                                if (isuPrivateRestClient.isFailedAuthCredentials(state)) {
                                    TextView isuAuthMessage = fragment.container().findViewById(R.id.isu_auth_message);
                                    if (isuAuthMessage != null) {
                                        isuAuthMessage.setText(isuPrivateRestClient.getFailedMessage(activity, code, state));
                                    }
                                }
                            }, throwable -> {
                                loadFailed();
                            });
                            return;
                        }
                        thread.runOnUI(ISA, () -> {
                            fragment.draw(R.layout.state_failed_button);
                            TextView message = fragment.container().findViewById(R.id.try_again_message);
                            if (message != null) {
                                message.setText(isuPrivateRestClient.getFailedMessage(activity, code, state));
                            }
                            View reload = fragment.container().findViewById(R.id.try_again_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                        }, throwable -> {
                            loadFailed();
                        });
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(ISA, () -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(isuPrivateRestClient.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public SSAssignedList newInstance() {
                    return new SSAssignedList();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(ISA, () -> {
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

    protected void display() {
        thread.run(ISA, () -> {
            log.v(TAG, "display");
            SSAssignedList data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            ScholarshipAssignedRVA adapter = new ScholarshipAssignedRVA(activity, data);
            thread.runOnUI(ISA, () -> {
                onToolbarSetup(fragment.toolbar());
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

    private void share() {
        thread.run(ISA, () -> {
            SSAssignedList data = getData();
            if (data == null || CollectionUtils.isEmpty(data.getList())) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Моя назначенная стипендия:");
            for (SSAssigned ssAssigned : data.getList()) {
                if (StringUtils.isBlank(ssAssigned.getContribution())) {
                    continue;
                }
                String ruble;
                switch (StringUtils.getWordDeclinationByNumber(NumberUtils.toDoubleInteger(ssAssigned.getSum()))) {
                    case 1: ruble = activity.getString(R.string.ruble1); break;
                    case 2: ruble = activity.getString(R.string.ruble2); break;
                    case 3: ruble = activity.getString(R.string.ruble3); break;
                    default: ruble = ""; break;
                }
                sb.append("\n");
                sb.append(ssAssigned.getContribution());
                if (StringUtils.isNotBlank(ssAssigned.getType())) {
                    sb.append(" — ");
                    sb.append(ssAssigned.getType());
                }
                if (StringUtils.isNotBlank(ssAssigned.getSource())) {
                    sb.append(" (");
                    sb.append(ssAssigned.getSource());
                    sb.append(")");
                }
                if (StringUtils.isNotBlank(ssAssigned.getStart()) && StringUtils.isNotBlank(ssAssigned.getEnd())) {
                    sb.append(" (");
                    sb.append(ssAssigned.getStart());
                    sb.append(" — ");
                    sb.append(ssAssigned.getEnd());
                    sb.append(")");
                }
                sb.append(": ");
                sb.append(ssAssigned.getSum());
                sb.append(" ");
                sb.append(ruble.toLowerCase());
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_scholarship_assigned"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected String getCacheType() {
        return Storage.USER;
    }

    @Override
    protected String getCachePath() {
        return "scholarship#assigned";
    }

    @Override
    protected String getThreadToken() {
        return ISA;
    }
}
