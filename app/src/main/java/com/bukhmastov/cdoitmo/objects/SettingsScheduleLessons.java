package com.bukhmastov.cdoitmo.objects;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
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
                new ScheduleLessons(new Schedule.Handler() {
                    @Override
                    public void onSuccess(final JSONObject json, final boolean fromCache) {
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
                                                String ti = json.getString("title");
                                                query = q;
                                                if (t.equals("room")) {
                                                    title = activity.getString(R.string.room) + " " + ti;
                                                } else {
                                                    title = ti;
                                                }
                                                Log.v(TAG, "show | search action | onSuccess | done | query=" + query + " | title=" + title);
                                                toggleSearchState("selected");
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
                                                    for (int i = 0; i < schedule.length(); i++) {
                                                        arrayList.add(schedule.getJSONObject(i));
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
                    @Override
                    public void onFailure(int state) {
                        this.onFailure(0, null, state);
                    }
                    @Override
                    public void onFailure(final int statusCode, final Client.Headers headers, final int state) {
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
                }).search(activity, q);
            }
        });
    }
}
