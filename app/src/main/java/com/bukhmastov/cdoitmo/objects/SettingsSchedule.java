package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public abstract class SettingsSchedule {

    private static final String TAG = "SettingsSchedule";
    public interface Callback {
        void onDone(String value);
    }
    protected static Client.Request requestHandle = null;
    protected final ConnectedActivity activity;
    protected final Callback callback;
    protected final Preference preference;
    protected String query = null;
    protected String title = null;
    protected AutoCompleteTextView lsp_search = null;
    protected TeacherPickerAdapter teacherPickerAdapter = null;
    protected ViewGroup lsp_search_action = null;
    protected ViewGroup lsp_search_loading = null;
    protected ViewGroup lsp_search_selected = null;

    public SettingsSchedule(ConnectedActivity activity, Preference preference, Callback callback) {
        this.activity = activity;
        this.preference = preference;
        this.callback = callback;
    }

    public void show() {
        Static.T.runOnUiThread(() -> {
            try {
                final String value = Storage.pref.get(activity, preference.key, (String) preference.defaultValue);
                final ViewGroup layout = (ViewGroup) inflate(R.layout.layout_schedule_preference);
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
                        Static.T.runOnUiThread(() -> {
                            teacherPickerAdapter.clear();
                            lsp_search.dismissDropDown();
                        });
                        Static.T.runThread(() -> {
                            if (requestHandle != null) {
                                requestHandle.cancel();
                            }
                        });
                    }
                });
                lsp_search_action.setOnClickListener(view -> Static.T.runThread(() -> {
                    final String query = lsp_search.getText().toString().trim();
                    Log.v(TAG, "show | search action | clicked | query=" + query);
                    if (!query.isEmpty()) {
                        if (requestHandle != null) {
                            requestHandle.cancel();
                        }
                        search(query);
                    }
                }));
                lsp_search.setOnItemClickListener((parent, view, position, id) -> Static.T.runThread(() -> {
                    try {
                        Log.v(TAG, "show | search list selected");
                        final JSONObject item = teacherPickerAdapter.getItem(position);
                        if (item != null) {
                            query = item.getString("pid");
                            title = item.getString("person");
                            Log.v(TAG, "show | search list selected | query=" + query + " | title=" + title);
                            Static.T.runOnUiThread(() -> lsp_search.setText(title));
                            toggleSearchState("selected");
                        } else {
                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                }));
                lsp_radio_group.setOnCheckedChangeListener((group, checkedId) -> Static.T.runOnUiThread(() -> {
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
                    Static.error(e);
                }
                // show dialog
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.default_schedule)
                        .setView(layout)
                        .setPositiveButton(R.string.accept, (dialog, which) -> Static.T.runThread(() -> {
                            Log.v(TAG, "show | onPositiveButton | query=" + query + " | title=" + title);
                            try {
                                if (callback != null && query != null && title != null) {
                                    if (query.isEmpty()) {
                                        Static.snackBar(activity, activity.getString(R.string.need_to_choose_schedule));
                                    } else {
                                        callback.onDone(new JSONObject()
                                                .put("query", query)
                                                .put("title", title)
                                                .toString()
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                Static.error(e);
                            }
                        }))
                        .create().show();
            } catch (Exception e) {
                Static.error(e);
            }
        });
    }
    protected abstract void search(final String query);
    protected abstract String getHint();
    protected void search(final String q, final Schedule.ScheduleSearchProvider scheduleSearchProvider) {
        Static.T.runThread(() -> scheduleSearchProvider.onSearch(activity, q, new Schedule.Handler() {
            @Override
            public void onSuccess(final JSONObject json, final boolean fromCache) {
                Log.v(TAG, "show | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                toggleSearchState("action");
                Static.T.runThread(() -> {
                    if (json == null) {
                        Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                    } else {
                        try {
                            String t = json.getString("type");
                            String q1 = json.getString("query");
                            Log.v(TAG, "show | search action | onSuccess | type=" + t);
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
                                        Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                        toggleSearchState("selected");
                                    }
                                    break;
                                }
                                case "teachers": {
                                    teacherPickerAdapter.clear();
                                    final JSONArray schedule = json.getJSONArray("schedule");
                                    Log.v(TAG, "show | search action | onSuccess | type=" + t + " | length=" + schedule.length());
                                    if (schedule.length() == 1) {
                                        JSONObject item = schedule.getJSONObject(0);
                                        if (item != null) {
                                            query = item.getString("pid");
                                            title = item.getString("person");
                                            Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                            Static.T.runOnUiThread(() -> lsp_search.setText(title));
                                            toggleSearchState("selected");
                                        } else {
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    } else {
                                        final ArrayList<JSONObject> arrayList = new ArrayList<>();
                                        for (int i = 0; i < schedule.length(); i++) {
                                            arrayList.add(schedule.getJSONObject(i));
                                        }
                                        Static.T.runOnUiThread(() -> {
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
                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Static.error(e);
                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
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
                Log.v(TAG, "show | search action | onFailure | state=" + state);
                toggleSearchState("action");
                Static.T.runOnUiThread(() -> {
                    switch (state) {
                        case Client.FAILED_OFFLINE:
                        case Schedule.FAILED_OFFLINE: {
                            Static.snackBar(activity, activity.getString(R.string.offline_mode_on));
                            break;
                        }
                        case Client.FAILED_SERVER_ERROR: {
                            Static.snackBar(activity, Client.getFailureMessage(activity, statusCode));
                            break;
                        }
                        case Client.FAILED_CORRUPTED_JSON: {
                            Static.snackBar(activity, activity.getString(R.string.server_provided_corrupted_json));
                            break;
                        }
                        default: {
                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                            break;
                        }
                    }
                });
            }
            @Override
            public void onProgress(int state) {
                Log.v(TAG, "activatePartSchedule | search action | onProgress | state=" + state);
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
        Static.T.runOnUiThread(() -> {
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

    protected View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
