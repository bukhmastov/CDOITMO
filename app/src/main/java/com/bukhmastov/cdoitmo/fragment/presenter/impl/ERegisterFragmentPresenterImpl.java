package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectsRVA;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.ClearCacheEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ERegisterSubjectFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterFragmentPresenter;
import com.bukhmastov.cdoitmo.function.BiFunction;
import com.bukhmastov.cdoitmo.model.eregister.ERPoint;
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
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.ER;

public class ERegisterFragmentPresenterImpl extends ConnectedFragmentWithDataPresenterImpl<ERegister>
        implements ERegisterFragmentPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ERegisterFragment";
    private static final Pattern PATTERN_ATTESTATION = Pattern.compile("^.*зач[её]т$|^экзамен$|^тестирование$|^общий\\sрейтинг$", Pattern.CASE_INSENSITIVE);
    private String group;
    private int term;
    private boolean spinnerGroupBlocker = true, spinnerPeriodBlocker = true;

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
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ERegisterFragmentPresenterImpl() {
        super(ERegister.class);
        AppComponentProvider.getComponent().inject(this);
        eventBus.register(this);
    }

    @Event
    public void onClearCacheEvent(ClearCacheEvent event) {
        if (event.isNot(ClearCacheEvent.EREGISTER)) {
            return;
        }
        clearData();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(ER);
        thread.run(ER, () -> {
            log.v(TAG, "Fragment created");
            if (App.UNAUTHORIZED_MODE) {
                forbidden = true;
                log.w(TAG, "Fragment created | UNAUTHORIZED_MODE not allowed, closing fragment...");
                thread.runOnUI(ER, () -> fragment.close());
                return;
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
            group = storage.get(activity, Storage.CACHE, Storage.USER, "eregister#params#selected_group", "");
            term = -2;
        });
    }

    @Override
    public void onToolbarSetup(Menu menu) {
        try {
            thread.assertUI();
            if (menu == null) {
                return;
            }
            MenuItem info = menu.findItem(R.id.action_info);
            MenuItem share = menu.findItem(R.id.action_share);
            if (info != null) {
                info.setVisible(true);
                info.setOnMenuItemClickListener(item -> {
                    thread.runOnUI(ER, () -> {
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
            thread.run(ER, () -> {
                if (share == null) {
                    return;
                }
                List<String> subjects = makeShareData(makeSubjectsSet(getData()));
                if (CollectionUtils.isEmpty(subjects)) {
                    thread.runOnUI(ER, () -> share.setVisible(false));
                    return;
                }
                thread.runOnUI(ER, () -> {
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
        thread.run(ER, () -> {
            log.v(TAG, "refreshing");
            load(true);
        });
    }

    protected void load() {
        thread.run(ER, () -> {
            load(storagePref.get(activity, "pref_use_cache", true) ?
                    Integer.parseInt(storagePref.get(activity, "pref_dynamic_refresh", "0")) :
                    0);
        });
    }

    private void load(int refreshRate) {
        thread.run(ER, () -> {
            log.v(TAG, "load | refreshRate=" + refreshRate);
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
            if (cache.getTimestamp() + refreshRate * 3600000L < time.getTimeInMillis()) {
                load(true, cache);
            } else {
                load(false, cache);
            }
        });
    }

    private void load(boolean force) {
        thread.run(ER, () -> load(force, null));
    }

    private void load(boolean force, ERegister cached) {
        thread.run(ER, () -> {
            log.v(TAG, "load | force=" + (force ? "true" : "false"));
            if ((!force || Client.isOffline(activity)) && storagePref.get(activity, "pref_use_cache", true)) {
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
                thread.runOnUI(ER, () -> {
                    fragment.draw(R.layout.state_offline_text);
                    View reload = fragment.container().findViewById(R.id.offline_reload);
                    if (reload != null) {
                        reload.setOnClickListener(v -> load());
                    }
                });
                return;
            }
            deIfmoRestClient.get(activity, "eregister", null, new RestResponseHandler<ERegister>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, ERegister response) throws Exception {
                    log.v(TAG, "load | success | code=", code, " | response=", response == null ? "<null>" : "<notnull>");
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
                    try {
                        log.v(TAG, "load | failure ", state);
                        if (state == Client.FAILED_OFFLINE) {
                            if (getData() != null) {
                                display();
                                return;
                            }
                            thread.runOnUI(ER, () -> {
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
                        if (code == 204) {
                            if (getData() != null) {
                                display();
                            } else {
                                loadFailed();
                            }
                            return;
                        }
                        thread.runOnUI(ER, () -> {
                            fragment.draw(R.layout.state_failed_button);
                            TextView message = fragment.container().findViewById(R.id.try_again_message);
                            if (message != null) {
                                message.setText(deIfmoRestClient.getFailedMessage(activity, code, state));
                            }
                            View reload = fragment.container().findViewById(R.id.try_again_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                        }, throwable -> {
                            loadFailed();
                        });
                    } catch (Throwable throwable) {
                        loadFailed();
                    }
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(ER, () -> {
                        log.v(TAG, "load | progress ", state);
                        fragment.draw(R.layout.state_loading_text);
                        TextView message = fragment.container().findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(deIfmoRestClient.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public ERegister newInstance() {
                    return new ERegister();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadFailed() {
        thread.runOnUI(ER, () -> {
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

    protected void display() {
        thread.run(ER, () -> {
            log.v(TAG, "display");
            ERegister data = getData();
            if (data == null) {
                loadFailed();
                return;
            }
            applySelectedTermAndGroup(data);
            ERegisterSubjectsRVA adapter = new ERegisterSubjectsRVA(activity, makeSubjectsSet(data));
            adapter.setClickListener(R.id.subject, (v, subject) -> {
                thread.runOnUI(ER, () -> {
                    Bundle extras = new Bundle();
                    extras.putSerializable("subject", subject);
                    activity.openActivityOrFragment(ERegisterSubjectFragment.class, extras);
                }, throwable -> {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            thread.runOnUI(ER, () -> {
                onToolbarSetup(fragment.toolbar());
                fragment.draw(R.layout.layout_eregister);
                // set adapter to recycler view
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.VERTICAL, false);
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
                            thread.run(ER, () -> {
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
                            thread.run(ER, () -> {
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

    private void share() {
        thread.run(ER, () -> {
            TreeSet<ERSubject> subjects = makeSubjectsSet(getData());
            if (CollectionUtils.isEmpty(subjects)) {
                return;
            }
            List<String> dataShare = makeShareData(subjects);
            if (CollectionUtils.isEmpty(dataShare)) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Мой электронный журнал:").append("\n");
            for (String item : dataShare) {
                sb.append(item).append("\n");
            }
            eventBus.fire(new ShareTextEvent(sb.toString().trim(), "txt_eregister"));
        }, throwable -> {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        });
    }

    private List<String> makeShareData(AbstractSet<ERSubject> subjects) {
        List<String> shareData = new ArrayList<>();
        if (CollectionUtils.isEmpty(subjects)) {
            return shareData;
        }
        for (ERSubject subject : subjects) {
            Double points = null;
            if (CollectionUtils.isNotEmpty(subject.getPoints())) {
                for (ERPoint point : subject.getPoints()) {
                    Double max = point.getMax();
                    if (max != null && max == 100.0) {
                        points = point.getValue();
                        if (points != null) {
                            break;
                        }
                    }
                }
                if (points == null) {
                    for (ERPoint point : subject.getPoints()) {
                        if (StringUtils.isBlank(point.getName()) || point.getValue() == null) {
                            continue;
                        }
                        if (PATTERN_ATTESTATION.matcher(point.getName()).find()) {
                            points = point.getValue();
                            break;
                        }
                    }
                }
            }
            if (points == null || points == 0.0) {
                continue;
            }
            shareData.add(subject.getName() + ": " + NumberUtils.prettyDouble(points));
        }
        return shareData;
    }

    private TreeSet<ERSubject> makeSubjectsSet(ERegister data) {
        TreeSet<ERSubject> subjects = new TreeSet<>(ERSubject::compareTo);
        if (data == null) {
            return subjects;
        }
        for (ERYear erYear : data.getYears()) {
            if (Objects.equals(group, erYear.getGroup())) {
                subjects.addAll(getSubjectsForTerm(erYear, term));
                break;
            }
        }
        return subjects;
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
        return "eregister#core";
    }

    @Override
    protected String getThreadToken() {
        return ER;
    }
}
