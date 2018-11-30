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
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
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

import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class IsuScholarshipPaidDetailsFragmentPresenterImpl implements IsuScholarshipPaidDetailsFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuScholarshipPaidDetailsFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private SSDetailedList data = null;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
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
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem share = menu.findItem(R.id.action_share);
            if (share != null) {
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    share.setVisible(false);
                } else {
                    share.setVisible(true);
                    share.setOnMenuItemClickListener(item -> {
                        share(data);
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
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (!loaded) {
                loaded = true;
                if (data == null) {
                    Bundle bundle = fragment.getArguments();
                    if (bundle == null) {
                        log.v(TAG, "Fragment resumed | bundle is null");
                        loadFailed();
                        return;
                    }
                    year = bundle.getInt("year");
                    month = bundle.getInt("month");
                    if (year == 0 || month == 0) {
                        log.v(TAG, "Fragment resumed | year or month is zero | year=", year, " | month=", month);
                        loadFailed();
                        return;
                    }
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
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }

    private void load(int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            if (data == null || data.getTimestamp() + refresh_rate * 3600000L < time.getTimeInMillis()) {
                load(true);
            } else {
                load(false);
            }
        });
    }

    private void load(boolean force) {
        thread.run(() -> {
            log.v(TAG, "load | force=", force);
            if ((!force || !Client.isOnline(activity))) {
                try {
                    if (data != null) {
                        log.v(TAG, "load | from cache");
                        display();
                        return;
                    }
                } catch (Exception e) {
                    log.v(TAG, "load | failed to load from cache");
                }
            }
            if (App.OFFLINE_MODE) {
                if (data != null) {
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
                thread.runOnUI(() -> {
                    fragment.draw(R.layout.layout_isu_required);
                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                    if (openIsuAuth != null) {
                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    }
                });
                return;
            }
            if (year == 0 || month == 0) {
                thread.runOnUI(() -> fragment.draw(R.layout.state_failed_text));
                return;
            }
            isuPrivateRestClient.get(activity, "scholarship/details/%apikey%/%isutoken%/" + year + "/" + month, null, new RestResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, JSONObject obj, JSONArray arr) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=", statusCode, " | obj=", obj);
                        if (statusCode == 200 && obj != null) {
                            SSDetailedList data = new SSDetailedList().fromJson(obj);
                            data.setTimestamp(time.getTimeInMillis());
                            IsuScholarshipPaidDetailsFragmentPresenterImpl.this.data = data;
                            display();
                            return;
                        }
                        if (data != null) {
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
                                if (data != null) {
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
            if (data == null) {
                loadFailed();
                return;
            }
            ScholarshipPaidDetailsRVA adapter = new ScholarshipPaidDetailsRVA(activity, data);
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

    private void share(SSDetailedList data) {
        thread.run(() -> {
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
}
