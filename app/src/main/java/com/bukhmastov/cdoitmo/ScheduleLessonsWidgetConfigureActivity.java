package com.bukhmastov.cdoitmo;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleLessonsWidgetConfigureActivity extends AppCompatActivity implements ScheduleLessons.response {

    private static final String TAG = "ScheduleLessonsWidgetC";
    private static final String PREF_PREFIX_KEY = "widget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    static RequestHandle widgetRequestHandle = null;
    static ScheduleLessons scheduleLessons = null;
    private String query = null;
    private boolean darkTheme = false;
    private int updateTime = 0;
    private int textColorPrimary, colorBackgroundSnackBar;
    private float destiny;

    public ScheduleLessonsWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setResult(RESULT_CANCELED);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false)) setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.schedule_lessons_widget_configure);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            close(RESULT_CANCELED, null);
            return;
        }
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_widget));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.configure_widget);
        }
        DeIfmoRestClient.init();
        scheduleLessons = new ScheduleLessons(getBaseContext());
        scheduleLessons.setHandler(this);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textColorPrimary = obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary}).getColor(0, -1);
        getTheme().resolveAttribute(R.attr.colorBackgroundSnackBar, typedValue, true);
        colorBackgroundSnackBar = obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorBackgroundSnackBar}).getColor(0, -1);
        destiny = getResources().getDisplayMetrics().density;
        EditText slw_input = (EditText) findViewById(R.id.slw_input);
        if (slw_input != null) {
            slw_input.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && setQuery();
                }
            });
            String group = Storage.get(getBaseContext(), "group");
            if (!Objects.equals(group, "")) {
                slw_input.setText(group);
                setQuery();
            }
        }
        Switch slw_switch_theme = (Switch) findViewById(R.id.slw_switch_theme);
        if (slw_switch_theme != null){
            slw_switch_theme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    darkTheme = isChecked;
                }
            });
            slw_switch_theme.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false));
        }
        Spinner slw_spinner_update_time = (Spinner) findViewById(R.id.slw_spinner_update_time);
        if (slw_spinner_update_time != null) {
            final ArrayList<String> spinner_labels = new ArrayList<>();
            final ArrayList<Integer> spinner_values = new ArrayList<>();
            spinner_labels.add("Только вручную");   spinner_values.add(0);
            spinner_labels.add("Раз в 12 часов");   spinner_values.add(12);
            spinner_labels.add("Раз в сутки");      spinner_values.add(24);
            spinner_labels.add("Раз в неделю");     spinner_values.add(168);
            spinner_labels.add("Раз в 4 недели");   spinner_values.add(672);
            slw_spinner_update_time.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_layout_normal, spinner_labels));
            slw_spinner_update_time.setSelection(3);
            slw_spinner_update_time.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    updateTime = spinner_values.get(position);
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (query == null) {
                    snackBar("Необходимо выбрать расписание для показа");
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("query", query);
                        jsonObject.put("darkTheme", darkTheme);
                        jsonObject.put("updateTime", updateTime);
                        Context context = ScheduleLessonsWidgetConfigureActivity.this;
                        savePref(context, mAppWidgetId, "settings", jsonObject.toString());
                        ScheduleLessonsWidget.updateAppWidget(context, AppWidgetManager.getInstance(context), mAppWidgetId, false);
                        Intent resultValue = new Intent();
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        close(RESULT_OK, resultValue);
                    } catch (Exception e) {
                        snackBar(getString(R.string.failed_to_create_widget));
                    }
                }
            }
        });
    }

    @Override
    public void onProgress(int state) {
        switch (state) {
            case DeIfmoRestClient.STATE_HANDLING: loading(getString(R.string.loading)); break;
            case DeIfmoRestClient.STATE_AUTHORIZATION: loading(getString(R.string.authorization)); break;
            case DeIfmoRestClient.STATE_AUTHORIZED: loading(getString(R.string.authorized)); break;
        }
    }

    @Override
    public void onFailure(int state) {
        switch (state) {
            case DeIfmoRestClient.FAILED_OFFLINE:
            case DeIfmoRestClient.FAILED_TRY_AGAIN:
            case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
            case ScheduleLessons.FAILED_LOAD:
                toast(getString(R.string.widget_creation_failed));
                close(RESULT_CANCELED, null);
                break;
            case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
            case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                toast(getString(R.string.widget_auth_failed));
                close(RESULT_CANCELED, null);
                break;
        }
    }

    @Override
    public void onSuccess(JSONObject json) {
        try {
            if (json == null) throw new NullPointerException("json cannot be null");
            if(Objects.equals(json.getString("type"), "teacher_picker")){
                JSONArray teachers = json.getJSONArray("teachers");
                if (teachers.length() > 0) {
                    if (teachers.length() == 1) {
                        JSONObject teacher = teachers.getJSONObject(0);
                        query = teacher.getString("scope");
                        found("Установлено расписание преподавателя" + " \"" + teacher.getString("name") + "\"");
                    } else {
                        FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
                        if (slw_container == null) throw new NullPointerException("slw_container cannot be null");
                        ListView listView = new ListView(getBaseContext());
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
                        listView.setAdapter(new TeacherPickerListView(this, teachersMap));
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                HashMap<String, String> teacherMap = teachersMap.get(position);
                                query = teacherMap.get("scope");
                                found("Установлено расписание преподавателя" + " \"" + teacherMap.get("name") + "\"");
                            }
                        });
                        slw_container.removeAllViews();
                        slw_container.addView(listView);
                    }
                } else {
                    query = null;
                    found(getString(R.string.schedule_not_found));
                }
            } else {
                if (json.getJSONArray("schedule").length() > 0) {
                    query = json.getString("scope");
                    switch(json.getString("type")){
                        case "group": found("Установлено расписание группы" + " \"" + query + "\""); break;
                        case "room": found("Установлено расписание аудитории" + " \"" + query + "\""); break;
                        default:
                            query = null;
                            found(getString(R.string.schedule_not_found));
                            break;
                    }
                } else {
                    query = null;
                    found(getString(R.string.schedule_not_found));
                }
            }
        } catch (Exception e){
            query = null;
            found(getString(R.string.schedule_not_found));
        }
    }

    @Override
    public void onNewHandle(RequestHandle requestHandle) {
        widgetRequestHandle = requestHandle;
    }

    private boolean setQuery(){
        EditText slw_input = (EditText) findViewById(R.id.slw_input);
        if (slw_input != null) {
            query = null;
            String search = slw_input.getText().toString().trim();
            if (!Objects.equals(search, "")) {
                scheduleLessons.search(search, false);
                slw_input.clearFocus();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void loading(String text){
        FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
        if (slw_container != null){
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), (int) (10 * destiny));
            ProgressBar progressBar = new ProgressBar(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            progressBar.setLayoutParams(lp);
            linearLayout.addView(progressBar);
            TextView textView = new TextView(this);
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * destiny), 0, (int) (10 * destiny));
            linearLayout.addView(textView);
            slw_container.removeAllViews();
            slw_container.addView(linearLayout);
        }
    }
    private void found(String text){
        FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
        if (slw_container != null){
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), (int) (10 * destiny));
            TextView textView = new TextView(this);
            textView.setText(text);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setTextColor(textColorPrimary);
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(0, (int) (10 * destiny), 0, (int) (10 * destiny));
            linearLayout.addView(textView);
            slw_container.removeAllViews();
            slw_container.addView(linearLayout);
        }
    }

    private void toast(String text){
        Toast.makeText(getBaseContext(), text, Toast.LENGTH_SHORT).show();
    }
    private void snackBar(String text){
        View content = findViewById(android.R.id.content);
        if (content != null) {
            Snackbar snackbar = Snackbar.make(content, text, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(colorBackgroundSnackBar);
            snackbar.show();
        }
    }
    private void close(int result, Intent intent){
        if (intent == null){
            setResult(result);
        } else {
            setResult(result, intent);
        }
        if (widgetRequestHandle != null) widgetRequestHandle.cancel(true);
        finish();
    }

    static void savePref(Context context, int appWidgetId, String type, String text) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_PREFIX_KEY + "_" + appWidgetId + "_" + type, text);
        editor.apply();
    }
    static String getPref(Context context, int appWidgetId, String type) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_PREFIX_KEY + "_" + appWidgetId + "_" + type, null);
    }
    static void deletePref(Context context, int appWidgetId, String type) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(PREF_PREFIX_KEY + "_" + appWidgetId + "_" + type);
        editor.apply();
    }
}

