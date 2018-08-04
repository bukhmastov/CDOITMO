package com.bukhmastov.cdoitmo.object.impl;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.inject.Inject;

public abstract class SettingsSchedule {

    private static final String TAG = "SettingsSchedule";
    public interface Callback {
        void onDone(String value);
    }
    protected ConnectedActivity activity;
    protected Callback callback;
    protected Preference preference;
    protected static Client.Request requestHandle = null;
    protected String query = null;
    protected String title = null;
    protected AutoCompleteTextView lsp_search = null;
    protected TeacherPickerAdapter teacherPickerAdapter = null;
    protected ViewGroup lsp_search_action = null;
    protected ViewGroup lsp_search_loading = null;
    protected ViewGroup lsp_search_selected = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    StoragePref storagePref;
    @Inject
    NotificationMessage notificationMessage;

    public SettingsSchedule() {
        AppComponentProvider.getComponent().inject(this);
    }

    protected abstract void search(final String query);
    protected abstract String getHint();

    protected void show(ConnectedActivity activity, Preference preference, Callback callback) {
        this.activity = activity;
        this.preference = preference;
        this.callback = callback;
        thread.runOnUI(() -> {
            try {
                final String value = storagePref.get(activity, preference.key, (String) preference.defaultValue);
                final ViewGroup layout = (ViewGroup) inflate(R.layout.preference_schedule);
                final RadioGroup lsp_radio_group = layout.findViewById(R.id.lsp_radio_group);
                final ViewGroup lsp_schedule_chooser = layout.findViewById(R.id.lsp_schedule_chooser);
                lsp_search = layout.findViewById(R.id.lsp_search);
                lsp_search_action = layout.findViewById(R.id.lsp_search_action);
                lsp_search_loading = layout.findViewById(R.id.lsp_search_loading);
                lsp_search_selected = layout.findViewById(R.id.lsp_search_selected);
                teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
                teacherPickerAdapter.setNotifyOnChange(true);
                lsp_search.setAdapter(teacherPickerAdapter);
                lsp_search.setHint(getHint());
                lsp_search.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void afterTextChanged(Editable editable) {
                        toggleSearchState("action");
                        thread.runOnUI(() -> {
                            teacherPickerAdapter.clear();
                            lsp_search.dismissDropDown();
                        });
                        thread.run(() -> {
                            if (requestHandle != null) {
                                requestHandle.cancel();
                            }
                        });
                    }
                });
                lsp_search_action.setOnClickListener(view -> thread.run(() -> {
                    final String query = lsp_search.getText().toString().trim();
                    log.v(TAG, "show | search action | clicked | query=" + query);
                    if (!query.isEmpty()) {
                        if (requestHandle != null) {
                            requestHandle.cancel();
                        }
                        search(query);
                    }
                }));
                lsp_search.setOnItemClickListener((parent, view, position, id) -> thread.run(() -> {
                    try {
                        log.v(TAG, "show | search list selected");
                        final JSONObject item = teacherPickerAdapter.getItem(position);
                        if (item != null) {
                            query = item.getString("pid");
                            title = item.getString("person");
                            log.v(TAG, "show | search list selected | query=" + query + " | title=" + title);
                            thread.runOnUI(() -> lsp_search.setText(title));
                            toggleSearchState("selected");
                        } else {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    } catch (Exception e) {
                        log.exception(e);
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                lsp_radio_group.setOnCheckedChangeListener((group, checkedId) -> thread.runOnUI(() -> {
                    switch (group.getCheckedRadioButtonId()) {
                        // TODO uncomment, when personal schedule will be ready
                        /*case R.id.lsp_schedule_personal: {
                            query = "mine";
                            title = "";
                            lsp_schedule_chooser.setVisibility(View.GONE);
                            break;
                        }*/
                        case R.id.lsp_schedule_group: {
                            query = "auto";
                            title = "";
                            lsp_schedule_chooser.setVisibility(View.GONE);
                            break;
                        }
                        case R.id.lsp_schedule_defined: {
                            if ("mine".equals(query) || "auto".equals(query)) {
                                query = "";
                                title = "";
                            }
                            lsp_schedule_chooser.setVisibility(View.VISIBLE);
                            toggleSearchState("action");
                            lsp_search.setText(title == null ? "" : title);
                            lsp_search.requestFocus();
                            break;
                        }
                    }
                }));
                lsp_schedule_chooser.setVisibility(View.GONE);
                try {
                    final JSONObject json = new JSONObject(value);
                    switch (json.getString("query")) {
                        // TODO uncomment, when personal schedule will be ready
                        //case "mine": lsp_radio_group.check(R.id.lsp_schedule_personal); break;
                        case "auto": lsp_radio_group.check(R.id.lsp_schedule_group); break;
                        default: {
                            query = json.getString("query");
                            title = json.getString("title");
                            lsp_radio_group.check(R.id.lsp_schedule_defined);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.exception(e);
                }
                // show dialog
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.default_schedule)
                        .setView(layout)
                        .setPositiveButton(R.string.accept, (dialog, which) -> thread.run(() -> {
                            log.v(TAG, "show | onPositiveButton | query=" + query + " | title=" + title);
                            try {
                                if (callback != null && query != null && title != null) {
                                    if (query.isEmpty()) {
                                        notificationMessage.snackBar(activity, activity.getString(R.string.need_to_choose_schedule));
                                    } else {
                                        callback.onDone(new JSONObject()
                                                .put("query", query)
                                                .put("title", title)
                                                .toString()
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                log.exception(e);
                            }
                        }))
                        .create().show();
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }

    protected void search(final String q, final Schedule.ScheduleSearchProvider scheduleSearchProvider) {
        thread.run(() -> scheduleSearchProvider.onSearch(activity, q, new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                log.v(TAG, "show | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                toggleSearchState("action");
                thread.run(() -> {
                    if (json == null) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found));
                    } else {
                        try {
                            String t = json.getString("type");
                            String q1 = json.getString("query");
                            log.v(TAG, "show | search action | onSuccess | type=" + t);
                            switch (t) {
                                case "group": case "room": case "teacher": {
                                    if (json.getJSONArray("schedule").length() > 0) {
                                        String ti = json.getString("title");
                                        query = q1;
                                        if (t.equals("room")) {
                                            title = activity.getString(R.string.room) + " " + ti;
                                        } else {
                                            title = ti;
                                        }
                                        log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                        toggleSearchState("selected");
                                    }
                                    break;
                                }
                                case "teachers": {
                                    teacherPickerAdapter.clear();
                                    final JSONArray schedule = json.getJSONArray("schedule");
                                    log.v(TAG, "show | search action | onSuccess | type=" + t + " | length=" + schedule.length());
                                    if (schedule.length() == 1) {
                                        JSONObject item = schedule.getJSONObject(0);
                                        if (item != null) {
                                            query = item.getString("pid");
                                            title = item.getString("person");
                                            log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                            thread.runOnUI(() -> lsp_search.setText(title));
                                            toggleSearchState("selected");
                                        } else {
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    } else {
                                        final ArrayList<JSONObject> arrayList = new ArrayList<>();
                                        for (int i = 0; i < schedule.length(); i++) {
                                            arrayList.add(schedule.getJSONObject(i));
                                        }
                                        thread.runOnUI(() -> {
                                            teacherPickerAdapter.addAll(arrayList);
                                            teacherPickerAdapter.addTeachers(arrayList);
                                            if (arrayList.size() > 0) {
                                                lsp_search.showDropDown();
                                            }
                                        });
                                    }
                                    break;
                                }
                                default: {
                                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }
                });
            }
            @Override
            public void onFailure(int state) {
                this.onFailure(0, null, state);
            }
            @Override
            public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
                log.v(TAG, "show | search action | onFailure | state=" + state);
                toggleSearchState("action");
                thread.runOnUI(() -> {
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case Schedule.FAILED_OFFLINE: {
                            notificationMessage.snackBar(activity, activity.getString(R.string.offline_mode_on));
                            break;
                        }
                        case Client.FAILED_SERVER_ERROR: {
                            notificationMessage.snackBar(activity, Client.getFailureMessage(activity, statusCode));
                            break;
                        }
                        case Client.FAILED_CORRUPTED_JSON: {
                            notificationMessage.snackBar(activity, activity.getString(R.string.server_provided_corrupted_json));
                            break;
                        }
                        default: {
                            notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found));
                            break;
                        }
                    }
                });
            }
            @Override
            public void onProgress(int state) {
                log.v(TAG, "activatePartSchedule | search action | onProgress | state=" + state);
                toggleSearchState("loading");
            }
            @Override
            public void onNewRequest(Client.Request request) {
                requestHandle = request;
            }
            @Override
            public void onCancelRequest() {
                if (requestHandle != null) {
                    requestHandle.cancel();
                }
            }
        }));
    }

    protected void toggleSearchState(final String state) {
        thread.runOnUI(() -> {
            switch (state) {
                case "action":
                default: {
                    if (lsp_search_action != null) lsp_search_action.setVisibility(View.VISIBLE);
                    if (lsp_search_loading != null) lsp_search_loading.setVisibility(View.GONE);
                    if (lsp_search_selected != null) lsp_search_selected.setVisibility(View.GONE);
                    break;
                }
                case "loading": {
                    if (lsp_search_action != null) lsp_search_action.setVisibility(View.GONE);
                    if (lsp_search_loading != null) lsp_search_loading.setVisibility(View.VISIBLE);
                    if (lsp_search_selected != null) lsp_search_selected.setVisibility(View.GONE);
                    break;
                }
                case "selected": {
                    if (lsp_search_action != null) lsp_search_action.setVisibility(View.GONE);
                    if (lsp_search_loading != null) lsp_search_loading.setVisibility(View.GONE);
                    if (lsp_search_selected != null) lsp_search_selected.setVisibility(View.VISIBLE);
                    break;
                }
            }
        });
    }

    protected View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
