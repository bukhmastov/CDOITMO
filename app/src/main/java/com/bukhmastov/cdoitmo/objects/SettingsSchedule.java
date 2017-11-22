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
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

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
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String value = Storage.pref.get(activity, preference.key, (String) preference.defaultValue);
                    final ViewGroup layout = (ViewGroup) inflate(R.layout.layout_schedule_preference);
                    final RadioGroup lsp_radio_group = layout.findViewById(R.id.lsp_radio_group);
                    final ViewGroup lsp_schedule_chooser = layout.findViewById(R.id.lsp_schedule_chooser);
                    lsp_search = layout.findViewById(R.id.lsp_search);
                    lsp_search_action = layout.findViewById(R.id.lsp_search_action);
                    lsp_search_loading = layout.findViewById(R.id.lsp_search_loading);
                    lsp_search_selected = layout.findViewById(R.id.lsp_search_selected);
                    teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<JSONObject>());
                    teacherPickerAdapter.setNotifyOnChange(true);
                    lsp_search.setAdapter(teacherPickerAdapter);
                    lsp_search.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                        @Override
                        public void afterTextChanged(Editable editable) {
                            toggleSearchState("action");
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    teacherPickerAdapter.clear();
                                    lsp_search.dismissDropDown();
                                }
                            });
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (requestHandle != null) {
                                        requestHandle.cancel();
                                    }
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
                                        search(query);
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
                                            query = item.getString("pid");
                                            title = item.getString("person");
                                            Log.v(TAG, "show | search list selected | query=" + query + " | title=" + title);
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
                            .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Static.T.runThread(new Runnable() {
                                        @Override
                                        public void run() {
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
    protected abstract void search(final String query);
    protected void toggleSearchState(final String state) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    protected View inflate(int layoutId) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
    }
}
