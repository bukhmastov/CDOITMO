package com.bukhmastov.cdoitmo.objects;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsScheduleLessons extends SettingsSchedule {

    private static final String TAG = "SettingsScheduleLessons";

    public SettingsScheduleLessons(ConnectedActivity activity, Preference preference, Callback callback) {
        super(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                ScheduleLessons scheduleLessons = new ScheduleLessons(activity);
                scheduleLessons.setHandler(new ScheduleLessons.response() {
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                    @Override
                    public void onProgress(final int state) {
                        Log.v(TAG, "activatePartSchedule | search action | onProgress | state=" + state);
                        toggleSearchState("loading");
                    }
                    @Override
                    public void onFailure(final int state) {
                        Log.v(TAG, "show | search action | onFailure | state=" + state);
                        toggleSearchState("action");
                        Static.T.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                switch (state) {
                                    case IfmoRestClient.FAILED_OFFLINE:
                                    case ScheduleLessons.FAILED_OFFLINE: {
                                        Static.snackBar(activity, activity.getString(R.string.offline_mode_on));
                                        break;
                                    }
                                    case IfmoRestClient.FAILED_SERVER_ERROR: {
                                        Static.snackBar(activity, IfmoRestClient.getFailureMessage(activity, -1));
                                        break;
                                    }
                                    default: {
                                        Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                        break;
                                    }
                                }
                            }
                        });
                    }
                    @Override
                    public void onSuccess(final JSONObject json) {
                        Log.v(TAG, "show | search action | onSuccess | json=" + (json == null ? "null" : "notnull"));
                        toggleSearchState("action");
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                if (json == null) {
                                    Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                } else {
                                    try {
                                        String t = json.getString("type");
                                        String q = json.getString("query");
                                        Log.v(TAG, "show | search action | onSuccess | type=" + t);
                                        switch (t) {
                                            case "group": case "room": case "teacher": {
                                                String l = json.getString("label");
                                                query = q;
                                                switch (t) {
                                                    case "group": case "teacher": {
                                                        label = l;
                                                        break;
                                                    }
                                                    case "room": {
                                                        label = activity.getString(R.string.room) + " " + l;
                                                        break;
                                                    }
                                                }
                                                Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | label=" + label);
                                                toggleSearchState("selected");
                                                break;
                                            }
                                            case "teacher_picker": {
                                                teacherPickerAdapter.clear();
                                                final JSONArray list = json.getJSONArray("list");
                                                Log.v(TAG, "show | search action | onSuccess | type=" + t + " | length=" + list.length());
                                                if (list.length() == 1) {
                                                    JSONObject item = list.getJSONObject(0);
                                                    if (item != null) {
                                                        query = item.getString("pid");
                                                        label = item.getString("person");
                                                        Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | label=" + label);
                                                        Static.T.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                lsp_search.setText(label);
                                                            }
                                                        });
                                                        toggleSearchState("selected");
                                                    } else {
                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                    }
                                                } else {
                                                    final ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                    for (int i = 0; i < list.length(); i++) {
                                                        arrayList.add(list.getJSONObject(i));
                                                    }
                                                    Static.T.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            teacherPickerAdapter.addAll(arrayList);
                                                            teacherPickerAdapter.addTeachers(arrayList);
                                                            if (arrayList.size() > 0) {
                                                                lsp_search.showDropDown();
                                                            }
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
                            }
                        });
                    }
                });
                scheduleLessons.search(q);
            }
        });
    }
}
