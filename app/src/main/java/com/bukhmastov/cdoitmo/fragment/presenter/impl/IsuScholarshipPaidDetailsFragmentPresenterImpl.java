package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScholarshipPaidDetailsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuScholarshipPaidDetailsFragmentPresenter;
import com.bukhmastov.cdoitmo.model.scholarship.detailed.SSDetailed;
import com.bukhmastov.cdoitmo.model.scholarship.detailed.SSDetailedList;
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

import static com.bukhmastov.cdoitmo.util.Thread.ISPD;

public class IsuScholarshipPaidDetailsFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<SSDetailedList>
        implements IsuScholarshipPaidDetailsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuScholarshipPaidDetailsFragment";
    private int year = 0;
    private int month = 0;

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
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public IsuScholarshipPaidDetailsFragmentPresenterImpl() {
        super(SSDetailedList.class);
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
            thread.run(ISPD, () -> {
                if (share == null) {
                    return;
                }
                SSDetailedList data = getData();
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    thread.runOnUI(ISPD, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(ISPD, () -> {
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
    public void onResume() {
        thread.run(ISPD, () -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                if (setYearMonthFromArguments()) {
                    fragment.clearData();
                    clearData();
                    load();
                } else {
                    loadFailed();
                }
            } else {
                display();
            }
        });
    }

    @Override
    public void onRefresh() {
        thread.run(ISPD, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    protected void load() {
        thread.run(ISPD, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) :
                    0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(ISPD, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            SSDetailedList data = getData();
            if (data == null || data.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true);
            } else {
                load(false);
            }
        });
    }

    private void load(boolean force) {
        thread.run(ISPD, () -> {
            log.v(TAG, "load | force=", force);
            if ((!force || Client.isOffline(activity))) {
                try {
                    if (getData() != null) {
                        log.v(TAG, "load | from cache");
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
                thread.runOnUI(ISPD, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (StringUtils.isBlank(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""))) {
                thread.runOnUI(ISPD, () -> {
                    fragment.draw(R.layout.layout_isu_required);
                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                    if (openIsuAuth != null) {
                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    }
                });
                return;
            }
            if (year == 0 || month == 0) {
                thread.runOnUI(ISPD, () -> fragment.draw(R.layout.state_failed_text));
                return;
            }
            String url = "scholarship/details/%apikey%/%isutoken%/" + year + "/" + month;
            isuPrivateRestClient.get(activity, url, null, new RestResponseHandler<SSDetailedList>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, SSDetailedList response) throws Exception {
                    log.v(TAG, "load | success | code=", code, " | response=", response);
                    if (code == 200 && response != null) {
                        response.setTimestamp(time.getTimeInMillis());
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
                    thread.run(ISPD, () -> {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            thread.runOnUI(ISPD, () -> {
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
                            thread.runOnUI(ISPD, () -> {
                                fragment.draw(R.layout.layout_isu_required);
                                View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                                if (openIsuAuth != null) {
                                    openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                                }
                            }, throwable -> {
                                loadFailed();
                            });
                            return;
                        }
                        thread.runOnUI(ISPD, () -> {
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
                    thread.runOnUI(ISPD, () -> {
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
                public SSDetailedList newInstance() {
                    return new SSDetailedList();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(ISPD, () -> {
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
        thread.run(ISPD, () -> {
            log.v(TAG, "display");
            SSDetailedList data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            ScholarshipPaidDetailsRVA adapter = new ScholarshipPaidDetailsRVA(activity, data);
            thread.runOnUI(ISPD, () -> {
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
        thread.run(ISPD, () -> {
            SSDetailedList data = getData();
            if (data == null || CollectionUtils.isEmpty(data.getList())) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            String period = "";
            for (SSDetailed ssDetailed : data.getList()) {
                if (StringUtils.isNotBlank(ssDetailed.getMonthOfPayment())) {
                    period = ssDetailed.getMonthOfPayment();
                    break;
                }
            }
            if (StringUtils.isNotBlank(period)) {
                sb.append("Моя стипендия за ").append(period).append(":");
            } else {
                sb.append("Моя стипендия:");
            }
            for (SSDetailed ssDetailed : data.getList()) {
                if (StringUtils.isBlank(ssDetailed.getContribution())) {
                    continue;
                }
                String value = StringUtils.removeHtmlTags(ssDetailed.getValue());
                String ruble;
                switch (StringUtils.getWordDeclinationByNumber(NumberUtils.toDoubleInteger(value))) {
                    case 1: ruble = activity.getString(R.string.ruble1); break;
                    case 2: ruble = activity.getString(R.string.ruble2); break;
                    case 3: ruble = activity.getString(R.string.ruble3); break;
                    default: ruble = ""; break;
                }
                sb.append("\n");
                sb.append(StringUtils.removeHtmlTags(ssDetailed.getContribution()));
                if (StringUtils.isNotBlank(ssDetailed.getStart()) && StringUtils.isNotBlank(ssDetailed.getEnd())) {
                    sb.append(" (");
                    sb.append(ssDetailed.getStart());
                    sb.append(" — ");
                    sb.append(ssDetailed.getEnd());
                    sb.append(")");
                }
                sb.append(": ");
                sb.append(value);
                sb.append(" ");
                sb.append(ruble.toLowerCase());
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_scholarship_paid_details"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private boolean setYearMonthFromArguments() {
        Bundle bundle = fragment.getArguments();
        if (bundle == null) {
            log.v(TAG, "Fragment resumed | bundle is null");
            return false;
        }
        year = bundle.getInt("year");
        month = bundle.getInt("month");
        if (year == 0 || month == 0) {
            log.v(TAG, "Fragment resumed | year or month is zero | year=", year, " | month=", month);
            return false;
        }
        return true;
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
        return ISPD;
    }
}
