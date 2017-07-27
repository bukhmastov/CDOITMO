package com.bukhmastov.cdoitmo.preferences;

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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public abstract class SchedulePreference extends DialogPreference {

    protected static final String TAG = "SchedulePreference";
    protected static RequestHandle preferenceRequestHandle = null;
    protected String DEFAULT_VALUE = null;
    protected String query = null;
    protected String title = null;
    protected View preference_schedule = null;

    public SchedulePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    public SchedulePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public SchedulePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public SchedulePreference(Context context) {
        super(context);
        init();
    }
    private void init() {
        setDefaultValue();
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    protected void setValue(String query, String title) {
        this.query = query;
        this.title = title;
    }
    protected void setValue(String value) {
        try {
            JSONObject jsonValue = new JSONObject(value);
            query = jsonValue.getString("query");
            title = jsonValue.getString("title");
        } catch (JSONException e) {
            setDefaultValue();
        }
    }
    protected String getValue() {
        try {
            JSONObject jsonValue = new JSONObject();
            jsonValue.put("query", query);
            jsonValue.put("title", title);
            return jsonValue.toString();
        } catch (JSONException e) {
            return null;
        }
    }
    protected void setDefaultValue() {
        setValue("auto", "");
        DEFAULT_VALUE = getValue();
    }

    protected void persist(String value) {
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
    protected boolean setQuery() {
        if (preference_schedule == null) return false;
        EditText editText = (EditText) preference_schedule.findViewById(R.id.schedule_preference_edit_text);
        if (editText != null) {
            String search = editText.getText().toString().trim();
            if (!Objects.equals(search, "")) {
                search(search);
                editText.clearFocus();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    protected abstract void search(String search);
    protected void loading(String text) {
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * Static.destiny), (int) (10 * Static.destiny), (int) (16 * Static.destiny), (int) (10 * Static.destiny));
            ProgressBar progressBar = new ProgressBar(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            progressBar.setLayoutParams(lp);
            linearLayout.addView(progressBar);
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(Static.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * Static.destiny), 0, (int) (10 * Static.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    protected void failed(String text) {
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * Static.destiny), (int) (10 * Static.destiny), (int) (16 * Static.destiny), (int) (10 * Static.destiny));
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            imageView.setLayoutParams(lp);
            imageView.setImageDrawable(getContext().getDrawable(R.drawable.ic_warning));
            linearLayout.addView(imageView);
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(Static.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * Static.destiny), 0, (int) (10 * Static.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    protected void found(String text) {
        if (preference_schedule == null) return;
        FrameLayout schedule_preference_list = (FrameLayout) preference_schedule.findViewById(R.id.schedule_preference_list);
        if (schedule_preference_list != null){
            LinearLayout linearLayout = new LinearLayout(getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * Static.destiny), (int) (10 * Static.destiny), (int) (16 * Static.destiny), (int) (10 * Static.destiny));
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(Static.textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * Static.destiny), 0, (int) (10 * Static.destiny));
            linearLayout.addView(textView);
            schedule_preference_list.removeAllViews();
            schedule_preference_list.addView(linearLayout);
        }
    }
    public void onProgress(int state) {
        switch (state) {
            case IfmoClient.STATE_HANDLING: loading(getContext().getString(R.string.loading)); break;
        }
    }
    public void onFailure(int state) {
        switch (state) {
            case IfmoClient.FAILED_OFFLINE:
                failed(getContext().getString(R.string.device_offline_action_refused));
                break;
            case IfmoClient.FAILED_TRY_AGAIN:
                failed(getContext().getString(R.string.load_failed));
                break;
        }
    }
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
            Static.error(e);
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
    protected static class SavedState extends BaseSavedState {
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

