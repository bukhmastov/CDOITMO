package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.SubjectShowFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterFragmentPresenter;
import com.bukhmastov.cdoitmo.function.BiFunction;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;
import com.bukhmastov.cdoitmo.model.eregister.ERYear;
import com.bukhmastov.cdoitmo.model.eregister.ERegister;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import javax.inject.Inject;

public class ERegisterFragmentPresenterImpl implements ERegisterFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private ERegister data = null;
    private String group;
    private int term;
    private boolean spinnerGroupBlocker = true, spinnerPeriodBlocker = true;
    private boolean loaded = false;
    private Client.Request requestHandle = null;
    private boolean forbidden = false;

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
    DeIfmoRestClient deIfmoRestClient;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ERegisterFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.EREGISTER)) {
            return;
        }
        data = null;
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
            if (App.UNAUTHORIZED_MODE) {
                forbidden = true;
                log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
                thread.runOnUI(() -> fragment.close());
                return;
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            group = storage.get(activity, Storage.CACHE, Storage.USER, "eregister#params#selected_group", "");
            term = -2;
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment destroyed");
            loaded = false;
            if (activity != null && activity.toolbar != null) {
                MenuItem action_info = activity.toolbar.findItem(R.id.action_info);
                if (action_info != null) {
                    action_info.setVisible(false);
                }
            }
        });
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(TAG, "Fragment resumed");
            if (forbidden) {
                return;
            }
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
            if (activity != null && activity.toolbar != null) {
                thread.runOnUI(() -> {
                    MenuItem action_info = activity.toolbar.findItem(R.id.action_info);
                    if (action_info != null) {
                        action_info.setVisible(true);
                        action_info.setOnMenuItemClickListener(item -> {
                            thread.runOnUI(() -> {
                                if (activity.isFinishing() || activity.isDestroyed()) {
                                    return;
                                }
                                new AlertDialog.Builder(activity)
                                        .setIcon(R.drawable.ic_info_outline)
                                        .setTitle(R.string.e_journal)
                                        .setMessage(R.string.e_journal_help)
                                        .setNegativeButton(R.string.close, null)
                                        .create().show();
                            });
                            return false;
                        });
                    }
                });
            }
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
        thread.runOnUI(() -> {
            log.v(TAG, "Fragment paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onRefresh() {
        thread.runOnUI(() -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    private void load() {
        thread.run(() -> load(storagePref.get(activity, "pref_use_cache", true) ? Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) : 0));
    }

    private void load(final int refresh_rate) {
        thread.run(() -> {
            log.v(TAG, "load | refresh_rate=" + refresh_rate);
            if (!storagePref.get(activity, "pref_use_cache", true)) {
                load(false);
                return;
            }
            ERegister cache = getFromCache();
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

    private void load(final boolean force) {
        thread.run(() -> load(force, null));
    }

    private void load(final boolean force, final ERegister cached) {
        thread.run(() -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || !Client.isOnline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
                try {
                    ERegister cache = cached == null ? getFromCache() : cached;
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
            deIfmoRestClient.get(activity, "eregister", null, new RestResponseHandler() {
                @Override
                public void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject obj, final JSONArray arr) {
                    thread.run(() -> {
                        log.v(TAG, "load | success | statusCode=" + statusCode + " | obj=" + (obj == null ? "null" : "notnull"));
                        if (statusCode == 200 && obj != null) {
                            ERegister data = new ERegister().fromJson(obj);
                            data.setTimestamp(time.getTimeInMillis());
                            if (storagePref.get(activity, "pref_use_cache", true)) {
                                storage.put(activity, Storage.CACHE, Storage.USER, "eregister#core", data.toJsonString());
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
                public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                    thread.run(() -> {
                        log.v(TAG, "load | failure " + state);
                        switch (state) {
                            case DeIfmoRestClient.FAILED_OFFLINE:
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
                            case DeIfmoRestClient.FAILED_TRY_AGAIN:
                            case DeIfmoRestClient.FAILED_SERVER_ERROR:
                            case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
                                thread.runOnUI(() -> {
                                    fragment.draw(R.layout.state_failed_button);
                                    TextView message = fragment.container().findViewById(R.id.try_again_message);
                                    if (message != null) {
                                        switch (state) {
                                            case DeIfmoRestClient.FAILED_SERVER_ERROR:
                                                if (activity == null) {
                                                    message.setText(DeIfmoRestClient.getFailureMessage(statusCode));
                                                } else {
                                                    message.setText(DeIfmoRestClient.getFailureMessage(activity, statusCode));
                                                }
                                                break;
                                            case DeIfmoRestClient.FAILED_CORRUPTED_JSON:
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
                        }
                    }, throwable -> {
                        loadFailed();
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
                                case DeIfmoRestClient.STATE_HANDLING: loading_message.setText(R.string.loading); break;
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
                message.setText(R.string.eregister_load_failed_retry_in_minute);
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
            ERegister data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            applySelectedTermAndGroup(data);
            TreeSet<ERSubject> subjects = new TreeSet<>(ERSubject::compareTo);
            for (ERYear erYear : data.getYears()) {
                if (Objects.equals(group, erYear.getGroup())) {
                    subjects.addAll(getSubjectsForTerm(erYear, term));
                    break;
                }
            }
            ERegisterSubjectsRVA adapter = new ERegisterSubjectsRVA(activity, subjects);
            adapter.setClickListener(R.id.subject, (v, subject) -> {
                thread.run(() -> {
                    Bundle extras = new Bundle();
                    extras.putSerializable("subject", subject);
                    thread.runOnUI(() -> activity.openActivityOrFragment(SubjectShowFragment.class, extras));
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            thread.runOnUI(() -> {
                fragment.draw(R.layout.layout_eregister);
                // set adapter to recycler view
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
                RecyclerView recyclerView = fragment.container().findViewById(R.id.erl_list_view);
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
                // setup spinners
                Spinner spinner;
                int selection = 0, counter = 0;
                spinner = fragment.container().findViewById(R.id.erl_group_spinner);
                if (spinner != null) {
                    List<String> groupArr = new ArrayList<>();
                    List<String> groupLabelArr = new ArrayList<>();
                    for (ERYear erYear : data.getYears()) {
                        groupArr.add(erYear.getGroup());
                        groupLabelArr.add(erYear.getGroup() + " (" + erYear.getYearFirst() + "/" + erYear.getYearSecond() + ")");
                        if (Objects.equals(group, erYear.getGroup())) {
                            selection = counter;
                        }
                        counter++;
                    }
                    spinner.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_center_single_line, groupLabelArr));
                    spinner.setSelection(selection);
                    spinnerGroupBlocker = true;
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                            thread.run(() -> {
                                if (spinnerGroupBlocker) {
                                    spinnerGroupBlocker = false;
                                    return;
                                }
                                group = groupArr.get(position);
                                log.v(TAG, "Group selected | group=" + group);
                                storage.put(activity, Storage.CACHE, Storage.USER, "eregister#params#selected_group", group);
                                load(false);
                            });
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
                spinner = fragment.container().findViewById(R.id.erl_period_spinner);
                if (spinner != null) {
                    List<Integer> termArr = new ArrayList<>();
                    List<String> termLabelArr = new ArrayList<>();
                    selection = 2;
                    for (ERYear erYear : data.getYears()) {
                        if (Objects.equals(group, erYear.getGroup())) {
                            Integer first = getTerm(erYear, Math::min);
                            Integer second = getTerm(erYear, Math::max);
                            if (first != null) {
                                termArr.add(first);
                                termLabelArr.add(first + " " + activity.getString(R.string.semester));
                                if (term == first) {
                                    selection = 0;
                                }
                            }
                            if (second != null) {
                                termArr.add(second);
                                termLabelArr.add(second + " " + activity.getString(R.string.semester));
                                if (term == second) {
                                    selection = 1;
                                }
                            }
                            termArr.add(-1);
                            termLabelArr.add(activity.getString(R.string.year));
                            break;
                        }
                    }
                    spinner.setAdapter(new ArrayAdapter<>(activity, R.layout.spinner_center_single_line, termLabelArr));
                    spinner.setSelection(selection);
                    spinnerPeriodBlocker = true;
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(final AdapterView<?> parent, final View item, final int position, final long selectedId) {
                            thread.run(() -> {
                                if (spinnerPeriodBlocker) {
                                    spinnerPeriodBlocker = false;
                                    return;
                                }
                                term = termArr.get(position);
                                log.v(TAG, "Period selected | period=" + termLabelArr.get(position));
                                load(false);
                            });
                        }
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
                // show update time
                notificationMessage.showUpdateTime(activity, data.getTimestamp(), NotificationMessage.LENGTH_MOMENTUM, true);
            }, throwable -> {
                loadFailed();
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void applySelectedTermAndGroup(ERegister data) {
        log.v(TAG, "applySelectedTermAndGroup");
        final Calendar now = time.getCalendar();
        final int year = now.get(Calendar.YEAR);
        final int month = now.get(Calendar.MONTH);
        String currentGroup = "";
        int currentTerm = -1, maxYear = 0;
        for (ERYear erYear : data.getYears()) {
            if (!group.isEmpty() && Objects.equals(group, erYear.getGroup())) {
                // мы нашли назначенную группу
                group = erYear.getGroup();
                // теперь проверяем семестр
                boolean isTermOk = false;
                if (term == -2) {
                    term = selectTerm(erYear);
                    isTermOk = true;
                } else {
                    for (ERSubject erSubject : erYear.getSubjects()) {
                        if (term != -1 && term == erSubject.getTerm()) {
                            isTermOk = true;
                            break;
                        }
                    }
                }
                if (!isTermOk) {
                    this.term = -1;
                }
            } else {
                // группа до сих пор не найдена
                if (StringUtils.isBlank(currentGroup)) {
                    if (year == (month > Calendar.AUGUST ? erYear.getYearFirst() : erYear.getYearSecond())) {
                        currentGroup = erYear.getGroup();
                        currentTerm = selectTermBasedOnPreference(erYear);
                    }
                }
                if (maxYear < erYear.getYearFirst()) {
                    maxYear = erYear.getYearFirst();
                }
            }
        }
        if (StringUtils.isBlank(group)) {
            if (StringUtils.isNotBlank(currentGroup)) {
                term = currentTerm;
                group = currentGroup;
            } else {
                term = -1;
                for (ERYear erYear : data.getYears()) {
                    if (erYear.getYearFirst() == maxYear) {
                        group = erYear.getGroup();
                        break;
                    }
                }
            }
        }
    }

    private Integer selectTerm(ERYear erYear) {
        final Calendar now = time.getCalendar();
        final int year = now.get(Calendar.YEAR);
        final int month = now.get(Calendar.MONTH);
        if (year == (month > Calendar.AUGUST ? erYear.getYearFirst() : erYear.getYearSecond())) {
            return selectTermBasedOnPreference(erYear);
        }
        return -1;
    }

    private Integer selectTermBasedOnPreference(ERYear erYear) {
        final Calendar now = time.getCalendar();
        final int month = now.get(Calendar.MONTH);
        switch (Integer.parseInt(storagePref.get(activity, "pref_e_journal_term", "0"))) {
            default: case 0: {
                //noinspection ConstantConditions
                return getTerm(erYear, (term1, term2) -> {
                    if (month > Calendar.AUGUST || month == Calendar.JANUARY) {
                        return Math.min(term1, term2);
                    } else {
                        return Math.max(term1, term2);
                    }
                });
            }
            case 1: {
                //noinspection ConstantConditions
                return getTerm(erYear, Math::min);
            }
            case 2: {
                //noinspection ConstantConditions
                return getTerm(erYear, Math::max);
            }
            case 3: {
                return -1;
            }
        }
    }

    private Collection<ERSubject> getSubjectsForTerm(ERYear year, Integer term) {
        if (term == null) {
            return new ArrayList<>();
        }
        Collection<ERSubject> subjects = new ArrayList<>();
        for (ERSubject subject : year.getSubjects()) {
            if (term == -1 || Objects.equals(term, subject.getTerm())) {
                subjects.add(subject);
            }
        }
        return subjects;
    }

    private Integer getTerm(ERYear year, BiFunction<Integer, Integer, Integer> termSelector) {
        Integer term1 = null, term2 = null;
        for (ERSubject subject : year.getSubjects()) {
            if (term1 == null && !Objects.equals(subject.getTerm(), term1)) {
                term1 = subject.getTerm();
            } else if (term2 == null && !Objects.equals(subject.getTerm(), term1) && !Objects.equals(subject.getTerm(), term2)) {
                term2 = subject.getTerm();
            }
            if (term1 != null && term2 != null) {
                break;
            }
        }
        Integer term;
        if (term1 == null && term2 == null) {
            return null;
        } else if (term1 == null) {
            term = term2;
        } else if (term2 == null) {
            term = term1;
        } else {
            term = termSelector.apply(term1, term2);
        }
        return term;
    }

    private ERegister getFromCache() {
        thread.assertNotUI();
        String cache = storage.get(activity, Storage.CACHE, Storage.USER, "eregister#core").trim();
        if (StringUtils.isBlank(cache)) {
            return null;
        }
        try {
            return new ERegister().fromJsonString(cache);
        } catch (Exception e) {
            storage.delete(activity, Storage.CACHE, Storage.USER, "eregister#core");
            return null;
        }
    }

    private void setData(ERegister data) {
        thread.assertNotUI();
        try {
            this.data = data;
            fragment.storeData(fragment, data.toJsonString());
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private ERegister getData() {
        thread.assertNotUI();
        if (data != null) {
            return data;
        }
        try {
            String stored = fragment.restoreData(fragment);
            if (stored != null && !stored.isEmpty()) {
                data = new ERegister().fromJsonString(stored);
                return data;
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return null;
    }
}
