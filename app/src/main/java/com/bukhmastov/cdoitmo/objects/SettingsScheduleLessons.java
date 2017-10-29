package com.bukhmastov.cdoitmo.objects;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsScheduleLessons {

    private static final String TAG = "SettingsScheduleLessons";
    public interface Callback {
        void onDone(String value);
    }
    private static Client.Request requestHandle = null;
    private ConnectedActivity activity;
    private Callback callback = null;
    private Preference preference = null;
    private String query = null;
    private String label = null;

    public SettingsScheduleLessons(ConnectedActivity activity, Preference preference, Callback callback) {
        this.activity = activity;
        this.preference = preference;
        this.callback = callback;
    }

    public void show() {
        final SettingsScheduleLessons self = this;
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String value = Storage.pref.get(activity, preference.key, (String) preference.defaultValue);
                    final ViewGroup layout = (ViewGroup) inflate(R.layout.layout_schedule_preference);
                    final RadioGroup lsp_radio_group = layout.findViewById(R.id.lsp_radio_group);
                    final ViewGroup lsp_schedule_chooser = layout.findViewById(R.id.lsp_schedule_chooser);
                    final AutoCompleteTextView lsp_search = layout.findViewById(R.id.lsp_search);
                    final ViewGroup lsp_search_action = layout.findViewById(R.id.lsp_search_action);
                    final ViewGroup lsp_search_loading = layout.findViewById(R.id.lsp_search_loading);
                    final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<JSONObject>());
                    teacherPickerAdapter.setNotifyOnChange(true);
                    lsp_search.setAdapter(teacherPickerAdapter);
                    lsp_search.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void afterTextChanged(Editable editable) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    teacherPickerAdapter.clear();
                                    lsp_search.dismissDropDown();
                                }
                            });
                        }
                    });
                    lsp_search_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    final String query = lsp_search.getText().toString().trim();
                                    Log.v(TAG, "show | search action | clicked | query=" + query);
                                    if (!query.isEmpty()) {
                                        if (requestHandle != null) {
                                            requestHandle.cancel();
                                        }
                                        ScheduleLessons scheduleLessons = new ScheduleLessons(activity);
                                        scheduleLessons.setHandler(new ScheduleLessons.response() {
                                            @Override
                                            public void onNewRequest(Client.Request request) {
                                                requestHandle = request;
                                            }
                                            @Override
                                            public void onProgress(final int state) {
                                                Log.v(TAG, "activatePartSchedule | search action | onProgress | state=" + state);
                                                Static.T.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        lsp_search_loading.setVisibility(View.VISIBLE);
                                                        lsp_search_action.setVisibility(View.GONE);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onFailure(final int state) {
                                                Log.v(TAG, "show | search action | onFailure | state=" + state);
                                                Static.T.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        lsp_search_loading.setVisibility(View.GONE);
                                                        lsp_search_action.setVisibility(View.VISIBLE);
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
                                                Static.T.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        lsp_search_loading.setVisibility(View.GONE);
                                                        lsp_search_action.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                                Static.T.runThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (json == null) {
                                                            Static.snackBar(activity, activity.getString(R.string.schedule_not_found));
                                                        } else {
                                                            try {
                                                                String type = json.getString("type");
                                                                String query = json.getString("query");
                                                                Log.v(TAG, "show | search action | onSuccess | type=" + type);
                                                                switch (type) {
                                                                    case "group": case "room": case "teacher": {
                                                                        String label = json.getString("label");
                                                                        self.query = query;
                                                                        switch (type) {
                                                                            case "group": case "teacher": {
                                                                                self.label = label;
                                                                                break;
                                                                            }
                                                                            case "room": {
                                                                                self.label = activity.getString(R.string.room) + " " + label;
                                                                                break;
                                                                            }
                                                                        }
                                                                        Log.v(TAG, "show | search action | onSuccess | done | query=" + self.query + " | label=" + self.label);
                                                                        break;
                                                                    }
                                                                    case "teacher_picker": {
                                                                        teacherPickerAdapter.clear();
                                                                        final JSONArray list = json.getJSONArray("list");
                                                                        Log.v(TAG, "show | search action | onSuccess | type=" + type + " | length=" + list.length());
                                                                        if (list.length() == 1) {
                                                                            JSONObject item = list.getJSONObject(0);
                                                                            if (item != null) {
                                                                                self.query = item.getString("pid");
                                                                                self.label = item.getString("person");
                                                                                Log.v(TAG, "show | search action | onSuccess | done | query=" + self.query + " | label=" + self.label);
                                                                                Static.T.runOnUiThread(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        lsp_search.setText(self.label);
                                                                                    }
                                                                                });
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
                                        scheduleLessons.search(query);
                                    }
                                }
                            });
                        }
                    });
                    lsp_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.v(TAG, "show | search list selected");
                                        final JSONObject item = teacherPickerAdapter.getItem(position);
                                        if (item != null) {
                                            self.query = item.getString("pid");
                                            self.label = item.getString("person");
                                            Log.v(TAG, "show | search list selected | query=" + self.query + " | label=" + self.label);
                                            Static.T.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    lsp_search.setText(self.label);
                                                }
                                            });
                                        } else {
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                    lsp_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(final RadioGroup group, final @IdRes int checkedId) {
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switch (group.getCheckedRadioButtonId()) {
                                        // TODO uncomment, when personal schedule will be ready
                                        /*case R.id.lsp_schedule_personal: {
                                            self.query = "mine";
                                            self.label = "";
                                            lsp_schedule_chooser.setVisibility(View.GONE);
                                            break;
                                        }*/
                                        case R.id.lsp_schedule_group: {
                                            self.query = "auto";
                                            self.label = "";
                                            lsp_schedule_chooser.setVisibility(View.GONE);
                                            break;
                                        }
                                        case R.id.lsp_schedule_defined: {
                                            if ("mine".equals(self.query) || "auto".equals(self.query)) {
                                                self.query = "";
                                                self.label = "";
                                            }
                                            lsp_schedule_chooser.setVisibility(View.VISIBLE);
                                            lsp_search_loading.setVisibility(View.GONE);
                                            lsp_search_action.setVisibility(View.VISIBLE);
                                            lsp_search.setText(self.label == null ? "" : self.label);
                                            lsp_search.requestFocus();
                                            break;
                                        }
                                    }
                                }
                            });
                        }
                    });
                    lsp_schedule_chooser.setVisibility(View.GONE);
                    try {
                        final JSONObject json = new JSONObject(value);
                        switch (json.getString("query")) {
                            // TODO uncomment, when personal schedule will be ready
                            //case "mine": lsp_radio_group.check(R.id.lsp_schedule_personal); break;
                            case "auto": lsp_radio_group.check(R.id.lsp_schedule_group); break;
                            default: {
                                self.query = json.getString("query");
                                self.label = json.getString("title");
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
                            .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Static.T.runThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.v(TAG, "show | onPositiveButton | query=" + self.query + " | label=" + self.label);
                                            try {
                                                if (self.callback != null && self.query != null && self.label != null) {
                                                    if (self.query.isEmpty()) {
                                                        Static.snackBar(activity, activity.getString(R.string.need_to_choose_schedule));
                                                    } else {
                                                        self.callback.onDone(new JSONObject()
                                                                .put("query", self.query)
                                                                .put("title", self.label)
                                                                .toString()
                                                        );
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Static.error(e);
                                            }
                                        }
                                    });
                                }
                            })
                            .create().show();
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }

    private View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
