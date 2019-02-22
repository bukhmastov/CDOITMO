package com.bukhmastov.cdoitmo.object.impl;

import android.app.AlertDialog;
import android.content.Context;
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
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.entity.SettingsQuery;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import androidx.annotation.LayoutRes;

public abstract class SettingsSchedule<T extends ScheduleJsonEntity> extends SettingsScheduleBase implements Schedule.Handler<T> {

    private static final String TAG = "SettingsSchedule";

    protected ConnectedActivity activity;
    protected Consumer<String> callback;
    protected Preference preference;
    protected static Client.Request requestHandle = null;
    protected String query = null;
    protected String title = null;
    protected AutoCompleteTextView searchTextView = null;
    protected TeacherPickerAdapter teacherPickerAdapter = null;
    protected ViewGroup searchAction = null;
    protected ViewGroup searchLoading = null;
    protected ViewGroup searchSelected = null;

    public SettingsSchedule() {
        super();
    }

    protected abstract void search(String query);

    protected abstract String getHint();

    protected void show(ConnectedActivity activity, Preference preference, Consumer<String> callback) {
        this.activity = activity;
        this.preference = preference;
        this.callback = callback;
        thread.runOnUI(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            String value = storagePref.get(activity, preference.key, (String) preference.defaultValue);
            ViewGroup layout = (ViewGroup) inflate(R.layout.preference_schedule);
            RadioGroup radioGroup = layout.findViewById(R.id.lsp_radio_group);
            ViewGroup scheduleChooser = layout.findViewById(R.id.lsp_schedule_chooser);
            searchTextView = layout.findViewById(R.id.lsp_search);
            searchAction = layout.findViewById(R.id.lsp_search_action);
            searchLoading = layout.findViewById(R.id.lsp_search_loading);
            searchSelected = layout.findViewById(R.id.lsp_search_selected);
            teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
            teacherPickerAdapter.setNotifyOnChange(true);
            searchTextView.setAdapter(teacherPickerAdapter);
            searchTextView.setHint(getHint());
            searchTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void afterTextChanged(Editable editable) {
                    toggleSearchState("action");
                    thread.runOnUI(() -> {
                        teacherPickerAdapter.clear();
                        searchTextView.dismissDropDown();
                    });
                    thread.standalone(() -> {
                        if (requestHandle != null) {
                            requestHandle.cancel();
                        }
                    });
                }
            });
            searchAction.setOnClickListener(view -> thread.standalone(() -> {
                String query = searchTextView.getText().toString().trim();
                log.v(TAG, "show | search action | clicked | query=", query);
                if (!query.isEmpty()) {
                    if (requestHandle != null) {
                        requestHandle.cancel();
                    }
                    search(query);
                }
            }));
            searchTextView.setOnItemClickListener((parent, view, position, id) -> {
                thread.standalone(() -> {
                    log.v(TAG, "show | search list selected");
                    STeacher teacher = teacherPickerAdapter.getItem(position);
                    if (teacher == null) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        return;
                    }
                    query = teacher.getPersonId();
                    title = teacher.getPerson();
                    log.v(TAG, "show | search list selected | query=", query, " | title=", title);
                    thread.runOnUI(() -> searchTextView.setText(title));
                    toggleSearchState("selected");
                }, throwable -> {
                    log.exception(throwable);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                });
            });
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> thread.runOnUI(() -> {
                switch (group.getCheckedRadioButtonId()) {
                    case R.id.lsp_schedule_personal: {
                        query = "personal";
                        title = "";
                        scheduleChooser.setVisibility(View.GONE);
                        break;
                    }
                    case R.id.lsp_schedule_group: {
                        query = "auto";
                        title = "";
                        scheduleChooser.setVisibility(View.GONE);
                        break;
                    }
                    case R.id.lsp_schedule_defined: {
                        if ("personal".equals(query) || "auto".equals(query)) {
                            query = "";
                            title = "";
                        }
                        scheduleChooser.setVisibility(View.VISIBLE);
                        toggleSearchState("action");
                        searchTextView.setText(title == null ? "" : title);
                        searchTextView.requestFocus();
                        break;
                    }
                }
            }));
            scheduleChooser.setVisibility(View.GONE);
            try {
                SettingsQuery objValue = new SettingsQuery().fromJsonString(value);
                if (objValue != null && StringUtils.isNotBlank(objValue.getQuery())) {
                    switch (objValue.getQuery()) {
                        case "personal": radioGroup.check(R.id.lsp_schedule_personal); break;
                        case "auto": radioGroup.check(R.id.lsp_schedule_group); break;
                        default: {
                            query = objValue.getQuery();
                            title = objValue.getTitle();
                            radioGroup.check(R.id.lsp_schedule_defined);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.exception(e);
            }
            // show dialog
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.default_schedule)
                    .setView(layout)
                    .setPositiveButton(R.string.accept, (dialog, which) -> {
                        thread.standalone(() -> {
                            log.v(TAG, "show | onPositiveButton | query=", query, " | title=", title);
                            if (callback == null || query == null || title == null) {
                                return;
                            }
                            if (StringUtils.isBlank(query)) {
                                notificationMessage.snackBar(activity, activity.getString(R.string.need_to_choose_schedule));
                                return;
                            }
                            callback.accept(new SettingsQuery(query, title).toJsonString());
                        }, throwable -> {
                            log.exception(throwable);
                        });
                    })
                    .create().show();
        }, throwable -> {
            log.exception(throwable);
        });
    }

    @Override
    public void onFailure(int statusCode, Client.Headers headers, int state) {
        log.v(TAG, "search | onFailure | state=", state);
        toggleSearchState("action");
        switch (state) {
            case Client.FAILED_OFFLINE:
            case Schedule.FAILED_OFFLINE: notificationMessage.snackBar(activity, activity.getString(R.string.offline_mode_on)); break;
            case Client.FAILED_SERVER_ERROR: notificationMessage.snackBar(activity, Client.getFailureMessage(activity, statusCode)); break;
            case Client.FAILED_CORRUPTED_JSON: notificationMessage.snackBar(activity, activity.getString(R.string.server_provided_corrupted_json)); break;
            default: notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found)); break;
        }
    }

    @Override
    public void onProgress(int state) {
        log.v(TAG, "search | onProgress | state=", state);
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

    protected void toggleSearchState(String state) {
        thread.runOnUI(() -> {
            switch (state) {
                case "action":
                default: {
                    if (searchAction != null) searchAction.setVisibility(View.VISIBLE);
                    if (searchLoading != null) searchLoading.setVisibility(View.GONE);
                    if (searchSelected != null) searchSelected.setVisibility(View.GONE);
                    break;
                }
                case "loading": {
                    if (searchAction != null) searchAction.setVisibility(View.GONE);
                    if (searchLoading != null) searchLoading.setVisibility(View.VISIBLE);
                    if (searchSelected != null) searchSelected.setVisibility(View.GONE);
                    break;
                }
                case "selected": {
                    if (searchAction != null) searchAction.setVisibility(View.GONE);
                    if (searchLoading != null) searchLoading.setVisibility(View.GONE);
                    if (searchSelected != null) searchSelected.setVisibility(View.VISIBLE);
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
