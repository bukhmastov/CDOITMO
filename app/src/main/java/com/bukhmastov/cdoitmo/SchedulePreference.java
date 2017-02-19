package com.bukhmastov.cdoitmo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SchedulePreference extends DialogPreference implements ScheduleLessons.response {

    private static final String TAG = "SchedulePreference";
    static ScheduleLessons scheduleLessons = null;
    private static RequestHandle preferenceRequestHandle = null;
    private String DEFAULT_VALUE = null;
    private String query = null;
    private String title = null;
    private View preference_schedule = null;

    public SchedulePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultValue();
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
        scheduleLessons = new ScheduleLessons(getContext());
        scheduleLessons.setHandler(this);
    }

    private void setValue(String query, String title){
        this.query = query;
        this.title = title;
    }
    private void setValue(String value){
        try {
            JSONObject jsonValue = new JSONObject(value);
            query = jsonValue.getString("query");
            title = jsonValue.getString("title");
        } catch (JSONException e) {
            setDefaultValue();
        }
    }
    private String getValue(){
        try {
            JSONObject jsonValue = new JSONObject();
            jsonValue.put("query", query);
            jsonValue.put("title", title);
            return jsonValue.toString();
        } catch (JSONException e) {
            return null;
        }
    }
    private void setDefaultValue(){
        setValue("auto", "");
        DEFAULT_VALUE = getValue();
    }

    private void persist(String value){
        persistString(value);
        OnPreferenceChangeListener onPreferenceChangeListener = this.getOnPreferenceChangeListener();
        if (onPreferenceChangeListener != null) onPreferenceChangeListener.onPreferenceChange(this, value);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            setValue(this.getPersistedString(DEFAULT_VALUE));
        } else {
            setValue((String) defaultValue);
            persist(getValue());
        }
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Object value = a.getString(index);
        setValue((String) value);
        return value;
    }

    /* main */
    @Override
    protected View onCreateDialogView() {
        preference_schedule = ((LayoutInflater) getContext().getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.preference_schedule, null);
        return preference_schedule;
    }
    @Override
    protected void onBindDialogView(View view) {
        final RadioGroup radioTypePicker = (RadioGroup) preference_schedule.findViewById(R.id.radioTypePicker);
        final FrameLayout ps_content = (FrameLayout) preference_schedule.findViewById(R.id.ps_content);
        if (radioTypePicker != null) {
            radioTypePicker.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Dialog dialog = getDialog();
                    if (dialog != null) {
                        Window window = dialog.getWindow();
                        if (window != null) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                        }
                    }
                    if (preferenceRequestHandle != null) preferenceRequestHandle.cancel(true);
                    if (ps_content != null) ps_content.removeAllViews();
                    if (checkedId == R.id.ps_auto) {
                        setValue("auto", "");
                    } else if (checkedId == R.id.ps_defined) {
                        if (Objects.equals(query, "auto")) setValue("", "");
                        if (ps_content != null) {
                            ps_content.addView(((LayoutInflater) getContext().getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.preference_schedule_defined, null));
                            EditText editText = (EditText) preference_schedule.findViewById(R.id.schedule_preference_edit_text);
                            if (editText != null) {
                                editText.setOnKeyListener(new View.OnKeyListener() {
                                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                                        return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && setQuery();
                                    }
                                });
                                if (!Objects.equals(title, "")) {
                                    editText.setText(title.replace(getContext().getString(R.string.group), "").replace(getContext().getString(R.string.room), "").trim());
                                }
                            }
                        }
                    }
                }
            });
        }
        switch (query) {
            case "auto": ((RadioButton) preference_schedule.findViewById(R.id.ps_auto)).setChecked(true); break;
            default: ((RadioButton) preference_schedule.findViewById(R.id.ps_defined)).setChecked(true); break;
        }
        super.onBindDialogView(view);
    }
    private boolean setQuery() {
        if (preference_schedule == null) return false;
        EditText editText = (EditText) preference_schedule.findViewById(R.id.schedule_preference_edit_text);
        if (editText != null) {
            String search = editText.getText().toString().trim();
            if (!Objects.equals(search, "")) {
                scheduleLessons.search(search);
                editText.clearFocus();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    private void loading(String text){
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny), (int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny));
            ProgressBar progressBar = new ProgressBar(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            progressBar.setLayoutParams(lp);
            linearLayout.addView(progressBar);
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(MainActivity.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * MainActivity.destiny), 0, (int) (10 * MainActivity.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    private void failed(String text){
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny), (int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny));
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            imageView.setLayoutParams(lp);
            imageView.setImageDrawable(getContext().getDrawable(R.drawable.ic_warning));
            linearLayout.addView(imageView);
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(MainActivity.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * MainActivity.destiny), 0, (int) (10 * MainActivity.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    private void found(String text){
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny), (int) (16 * MainActivity.destiny), (int) (10 * MainActivity.destiny));
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(MainActivity.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * MainActivity.destiny), 0, (int) (10 * MainActivity.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    @Override
    public void onProgress(int state) {
        switch (state) {
            case DeIfmoRestClient.STATE_HANDLING: loading(getContext().getString(R.string.loading)); break;
            case DeIfmoRestClient.STATE_AUTHORIZATION: loading(getContext().getString(R.string.authorization)); break;
            case DeIfmoRestClient.STATE_AUTHORIZED: loading(getContext().getString(R.string.authorized)); break;
        }
    }
    @Override
    public void onFailure(int state) {
        switch (state) {
            case DeIfmoRestClient.FAILED_OFFLINE:
                failed(getContext().getString(R.string.device_offline_action_refused));
                break;
            case DeIfmoRestClient.FAILED_TRY_AGAIN:
            case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
            case ScheduleLessons.FAILED_LOAD:
                failed(getContext().getString(R.string.load_failed));
                break;
            case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
            case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                failed(getContext().getString(R.string.auth_failed));
                break;
        }
    }
    @Override
    public void onSuccess(JSONObject json) {
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            if (Objects.equals(json.getString("type"), "teacher_picker")) {
                JSONArray teachers = json.getJSONArray("teachers");
                if (teachers.length() > 0) {
                    if (teachers.length() == 1) {
                        JSONObject teacher = teachers.getJSONObject(0);
                        setValue(teacher.getString("scope"), teacher.getString("name"));
                        found(getContext().getString(R.string.schedule_teacher_set) + " \"" + teacher.getString("name") + "\"");
                    } else {
                        if (preference_schedule == null) return;
                        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
                        if (schedule_preference_list == null) throw new NullPointerException("slw_container cannot be null");
                        ListView listView = new ListView(getContext());
                        listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                        for (int i = 0; i < teachers.length(); i++) {
                            JSONObject teacher = teachers.getJSONObject(i);
                            HashMap<String, String> teacherMap = new HashMap<>();
                            teacherMap.put("name", teacher.getString("name"));
                            teacherMap.put("scope", teacher.getString("scope"));
                            teacherMap.put("id", teacher.getString("id"));
                            teachersMap.add(teacherMap);
                        }
                        listView.setAdapter(new TeacherPickerListView((Activity) getContext(), teachersMap));
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                HashMap<String, String> teacherMap = teachersMap.get(position);
                                setValue(teacherMap.get("scope"), teacherMap.get("name"));
                                found(getContext().getString(R.string.schedule_teacher_set) + " \"" + teacherMap.get("name") + "\"");
                            }
                        });
                        schedule_preference_list.removeAllViews();
                        schedule_preference_list.addView(listView);
                    }
                } else {
                    found(getContext().getString(R.string.schedule_not_found));
                }
            } else {
                if (json.getJSONArray("schedule").length() > 0) {
                    switch(json.getString("type")){
                        case "group":
                            setValue(json.getString("scope"), getContext().getString(R.string.group) + " " + json.getString("scope"));
                            found(getContext().getString(R.string.schedule_group_set) + " \"" + json.getString("scope") + "\"");
                            break;
                        case "room":
                            setValue(json.getString("scope"), getContext().getString(R.string.room) + " " + json.getString("scope"));
                            found(getContext().getString(R.string.schedule_room_set) + " \"" + json.getString("scope") + "\"");
                            break;
                        default:
                            found(getContext().getString(R.string.schedule_not_found));
                            break;
                    }
                } else {
                    found(getContext().getString(R.string.schedule_not_found));
                }
            }
        } catch (Exception e){
            found(getContext().getString(R.string.schedule_not_found));
        }
    }
    @Override
    public void onNewHandle(RequestHandle requestHandle) {
        preferenceRequestHandle = requestHandle;
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (preferenceRequestHandle != null) preferenceRequestHandle.cancel(true);
        String value = getValue();
        try {
            JSONObject jsonObject = new JSONObject(value);
            if (Objects.equals(jsonObject.getString("query"), "")) {
                setDefaultValue();
            } else {
                if (positiveResult) persist(value);
            }
        } catch (JSONException e){
            if (LoginActivity.errorTracker != null) LoginActivity.errorTracker.add(e);
        }
    }

    /* save state */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        // mNumberPicker.setValue(myState.value);
    }
    private static class SavedState extends BaseSavedState {
        String value;
        SavedState(Parcelable superState) {
            super(superState);
        }
        SavedState(Parcel source) {
            super(source);
            value = source.readString();
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(value);
        }
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}

