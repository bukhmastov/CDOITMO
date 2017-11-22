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

public class SettingsScheduleExams extends SettingsSchedule {

    private static final String TAG = "SettingsScheduleExams";

    public SettingsScheduleExams(ConnectedActivity activity, Preference preference, Callback callback) {
        super(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                ScheduleExams scheduleExams = new ScheduleExams(activity);
                scheduleExams.setHandler(new ScheduleExams.response() {
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
                                    case ScheduleExams.FAILED_OFFLINE: {
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
                                        String type = json.getString("type");
                                        Log.v(TAG, "show | search action | onSuccess | type=" + type);
                                        switch (type) {
                                            case "group":
                                            case "room":
                                            case "teacher": {
                                                if (json.getJSONArray("schedule").length() > 0) {
                                                    String q = json.getString("scope");
                                                    String t = json.getString("scope");
                                                    query = q;
                                                    if (type.equals("room")) {
                                                        title = activity.getString(R.string.room) + " " + t;
                                                    } else {
                                                        title = t;
                                                    }
                                                    Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                                    toggleSearchState("selected");
                                                }
                                                break;
                                            }
                                            case "teacher_picker": {
                                                teacherPickerAdapter.clear();
                                                final JSONArray list = json.getJSONArray("teachers");
                                                Log.v(TAG, "show | search action | onSuccess | type=" + type + " | length=" + list.length());
                                                if (list.length() == 1) {
                                                    JSONObject item = list.getJSONObject(0);
                                                    if (item != null) {
                                                        query = item.getString("scope");
                                                        title = item.getString("name");
                                                        Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                                        Static.T.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                lsp_search.setText(title);
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
                scheduleExams.search(q);
            }
        });
    }
}
