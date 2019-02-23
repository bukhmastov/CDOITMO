package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ScholarshipPaidRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.IsuScholarshipAssignedFragment;
import com.bukhmastov.cdoitmo.fragment.IsuScholarshipPaidDetailsFragment;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuScholarshipPaidFragmentPresenter;
import com.bukhmastov.cdoitmo.model.scholarship.paid.SSPaid;
import com.bukhmastov.cdoitmo.model.scholarship.paid.SSPaidList;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.ISP;

public class IsuScholarshipPaidFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<SSPaidList>
        implements IsuScholarshipPaidFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "IsuScholarshipPaidFragment";
    private static final DateFormat REQUEST_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);

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

    public IsuScholarshipPaidFragmentPresenterImpl() {
        super(SSPaidList.class);
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
            thread.run(ISP, () -> {
                if (share == null) {
                    return;
                }
                SSPaidList data = getData();
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    thread.runOnUI(ISP, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(ISP, () -> {
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
        thread.run(ISP, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    protected void load() {
        thread.run(ISP, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) :
                    0);
        });
    }

    private void load(int refresh_rate) {
        thread.run(ISP, () -> {
            log.v(TAG, "load | refresh_rate=", refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            SSPaidList cache = getFromCache();
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
        thread.run(ISP, () -> load(force, null));
    }

    private void load(boolean force, SSPaidList cached) {
        thread.run(ISP, () -> {
            log.v(TAG, "load | force=", force);
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    SSPaidList cache = cached == null ? getFromCache() : cached;
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
                thread.runOnUI(ISP, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            if (StringUtils.isBlank(storage.get(activity, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""))) {
                thread.runOnUI(ISP, () -> {
                    fragment.draw(R.layout.layout_isu_required);
                    View openIsuAuth = fragment.container().findViewById(R.id.open_isu_auth);
                    if (openIsuAuth != null) {
                        openIsuAuth.setOnClickListener(v -> activity.openActivity(ConnectedActivity.TYPE.STACKABLE, LinkedAccountsFragment.class, null));
                    }
                });
                return;
            }
            Calendar calendar = time.getCalendar();
            String to = REQUEST_DATE_FORMAT.format(calendar.getTime());
            calendar.add(Calendar.YEAR, -6);
            String from = REQUEST_DATE_FORMAT.format(calendar.getTime());
            isuPrivateRestClient.get(activity, "scholarship/payments/%apikey%/%isutoken%/" + from + "/" + to, null, new RestResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, JSONObject obj, JSONArray arr) throws Exception {
                    log.v(TAG, "load | success | statusCode=", statusCode, " | obj=", obj);
                    if (statusCode == 200 && obj != null) {
                        SSPaidList data = new SSPaidList().fromJson(obj);
                        data.setTimestamp(time.getTimeInMillis());
                        putToCache(data);
                        setData(data);
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
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    thread.run(ISP, () -> {
                        log.v(TAG, "load | failure ", state);
                        switch (state) {
                            case IsuPrivateRestClient.FAILED_OFFLINE:
                                if (getData() != null) {
                                    display();
                                    return;
                                }
                                thread.runOnUI(ISP, () -> {
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
                                thread.runOnUI(ISP, () -> {
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
                                thread.runOnUI(ISP, () -> {
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
                    thread.runOnUI(ISP, () -> {
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
        thread.runOnUI(ISP, () -> {
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
        thread.run(ISP, () -> {
            log.v(TAG, "display");
            SSPaidList data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            ScholarshipPaidRVA adapter = new ScholarshipPaidRVA(activity, data);
            adapter.setClickListener(R.id.scholarship_assigned_container, (v, ssPaid) -> {
                thread.runOnUI(ISP, () -> {
                    activity.openActivityOrFragment(IsuScholarshipAssignedFragment.class, null);
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            adapter.setClickListener(R.id.scholarship_paid_item, (v, ssPaid) -> {
                if (ssPaid == null || ssPaid.getMonth() == 0 || ssPaid.getYear() == 0) {
                    return;
                }
                thread.runOnUI(ISP, () -> {
                    Bundle extras = new Bundle();
                    extras.putInt("month", ssPaid.getMonth());
                    extras.putInt("year", ssPaid.getYear());
                    thread.runOnUI(ISP, () -> activity.openActivityOrFragment(IsuScholarshipPaidDetailsFragment.class, extras));
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            thread.runOnUI(ISP, () -> {
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
        thread.run(ISP, () -> {
            SSPaidList data = getData();
            if (data == null || CollectionUtils.isEmpty(data.getList())) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Моя полученная стипендия:");
            for (SSPaid ssPaid : data.getList()) {
                String ruble;
                switch (StringUtils.getWordDeclinationByNumber(NumberUtils.toDoubleInteger(ssPaid.getValue()))) {
                    case 1: ruble = activity.getString(R.string.ruble1); break;
                    case 2: ruble = activity.getString(R.string.ruble2); break;
                    case 3: ruble = activity.getString(R.string.ruble3); break;
                    default: ruble = ""; break;
                }
                sb.append("\n");
                sb.append(time.getMonth(activity, ssPaid.getMonth()));
                sb.append(" ");
                sb.append(ssPaid.getYear());
                sb.append(" — ");
                sb.append(ssPaid.getValue());
                sb.append(" ");
                sb.append(ruble.toLowerCase());
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_scholarship_paid"));
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
        return "scholarship#paid";
    }

    @Override
    protected String getThreadToken() {
        return ISP;
    }
}
