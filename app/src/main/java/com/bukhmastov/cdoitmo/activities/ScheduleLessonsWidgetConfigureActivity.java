package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerListView;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.bukhmastov.cdoitmo.widgets.ScheduleLessonsWidget;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ScheduleLessonsWidgetConfigureActivity extends AppCompatActivity implements ScheduleLessons.response {

    private static final String TAG = "SLWidgetConfigureActivity";
    private ScheduleLessonsWidgetConfigureActivity self = this;
    public int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public static RequestHandle widgetRequestHandle = null;
    public static ScheduleLessons scheduleLessons = null;
    private String query = null;
    private boolean darkTheme = false;
    private int updateTime = 168;
    private int textColorPrimary;
    private float destiny;

    public ScheduleLessonsWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "Activity created");
        FirebaseAnalyticsProvider.logCurrentScreen(this);
        setResult(RESULT_CANCELED);
        boolean isDarkTheme = Storage.pref.get(this, "pref_dark_theme", false);
        if (isDarkTheme) setTheme(R.style.AppTheme_Dark);
        setContentView(R.layout.schedule_lessons_widget_configure);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "wrong AppWidgetId provided by intent's extra");
            close(RESULT_CANCELED, null);
            return;
        }
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_widget));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.configure_schedule_widget);
        }
        scheduleLessons = new ScheduleLessons(this);
        scheduleLessons.setHandler(this);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textColorPrimary = obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary}).getColor(0, -1);
        destiny = getResources().getDisplayMetrics().density;
        EditText slw_input = (EditText) findViewById(R.id.slw_input);
        if (slw_input != null) {
            slw_input.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && setQuery();
                }
            });
            String group = Storage.file.perm.get(this, "user#group");
            if (!Objects.equals(group, "")) {
                slw_input.setText(group);
                setQuery();
            }
        }
        final Switch slw_switch_theme = (Switch) findViewById(R.id.slw_switch_theme);
        if (slw_switch_theme != null){
            slw_switch_theme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.v(TAG, "switch_theme clicked | darkTheme=" + (isChecked ? "true" : "false"));
                    darkTheme = isChecked;
                }
            });
            slw_switch_theme.setChecked(isDarkTheme);
        }
        final RelativeLayout slw_theme = (RelativeLayout) findViewById(R.id.slw_theme);
        if (slw_theme != null) {
            slw_theme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "slw_theme clicked");
                    if (slw_switch_theme != null){
                        darkTheme = !slw_switch_theme.isChecked();
                        slw_switch_theme.setChecked(darkTheme);
                    } else {
                        Log.w(TAG, "slw_switch_theme is null");
                    }
                }
            });
        }
        LinearLayout slw_update_time = (LinearLayout) findViewById(R.id.slw_update_time);
        if (slw_update_time != null) {
            slw_update_time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "slw_update_time clicked");
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScheduleLessonsWidgetConfigureActivity.this);
                    builder.setTitle(R.string.update_interval);
                    int select = 0;
                    switch (updateTime){
                        case 0: select = 0; break;
                        case 12: select = 1; break;
                        case 24: select = 2; break;
                        case 168: select = 3; break;
                        case 672: select = 4; break;
                    }
                    builder.setSingleChoiceItems(R.array.pref_widget_refresh_titles, select, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.v(TAG, "slw_update_time dialog selected " + which);
                            switch (which){
                                case 0: updateTime = 0; break;
                                case 1: updateTime = 12; break;
                                case 2: updateTime = 24; break;
                                case 3: updateTime = 168; break;
                                case 4: updateTime = 672; break;
                            }
                            updateTimeSummary();
                            dialog.dismiss();
                        }
                    });
                    builder.setCancelable(true);
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.v(TAG, "slw_update_time dialog dismissed");
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            });
        }
        updateTimeSummary();
        final Activity activity = this;
        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, "add_button clicked");
                if (query == null) {
                    Static.snackBar(activity, getString(R.string.need_to_pick_schedule));
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
                        FirebaseAnalyticsProvider.logEvent(
                                context,
                                FirebaseAnalyticsProvider.Event.WIDGET_INSTALL,
                                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.WIDGET_QUERY, query)
                        );
                    } catch (Exception e) {
                        Log.w(TAG, "failed to create widget");
                        Static.error(e);
                        Static.snackBar(activity, getString(R.string.failed_to_create_widget));
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    @Override
    public void onProgress(int state) {
        Log.v(TAG, "progress " + state);
        switch (state) {
            case IfmoRestClient.STATE_HANDLING: loading(getString(R.string.loading)); break;
        }
    }

    @Override
    public void onFailure(int state) {
        Log.v(TAG, "failure " + state);
        switch (state) {
            case IfmoRestClient.FAILED_OFFLINE:
            case IfmoRestClient.FAILED_TRY_AGAIN:
            case ScheduleLessons.FAILED_LOAD:
                Static.toast(this, getString(R.string.widget_creation_failed));
                close(RESULT_CANCELED, null);
                break;
        }
    }

    @Override
    public void onSuccess(final JSONObject json) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "success");
                try {
                    if (json == null) throw new NullPointerException("json cannot be null");
                    if (Objects.equals(json.getString("type"), "teacher_picker")) {
                        Log.v(TAG, "type=teacher_picker");
                        JSONArray teachers = json.getJSONArray("list");
                        if (teachers.length() > 0) {
                            if (teachers.length() == 1) {
                                JSONObject teacher = teachers.getJSONObject(0);
                                query = teacher.getString("pid");
                                Log.v(TAG, "found query=" + query);
                                found(getString(R.string.schedule_teacher_set) + " \"" + teacher.getString("person") + " (" + teacher.getString("post") + ")" + "\"");
                            } else {
                                FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
                                if (slw_container == null) throw new NullPointerException("slw_container cannot be null");
                                ListView listView = new ListView(self);
                                listView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                final ArrayList<HashMap<String, String>> teachersMap = new ArrayList<>();
                                for (int i = 0; i < teachers.length(); i++) {
                                    JSONObject teacher = teachers.getJSONObject(i);
                                    HashMap<String, String> teacherMap = new HashMap<>();
                                    teacherMap.put("pid", String.valueOf(teacher.getInt("pid")));
                                    teacherMap.put("person", teacher.getString("person"));
                                    teacherMap.put("post", teacher.getString("post"));
                                    teachersMap.add(teacherMap);
                                }
                                listView.setAdapter(new TeacherPickerListView(self, teachersMap));
                                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        HashMap<String, String> teacherMap = teachersMap.get(position);
                                        query = teacherMap.get("pid");
                                        Log.v(TAG, "found query=" + query);
                                        found(getString(R.string.schedule_teacher_set) + " \"" + teacherMap.get("person") + " (" + teacherMap.get("post") + ")" + "\"");
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
                        Log.v(TAG, "type=" + json.getString("type"));
                        if (json.getJSONArray("schedule").length() > 0) {
                            query = json.getString("query");
                            switch (json.getString("type")) {
                                case "group":
                                    Log.v(TAG, "found query=" + query);
                                    found(getString(R.string.schedule_group_set) + " \"" + json.getString("label") + "\"");
                                    break;
                                case "room":
                                    Log.v(TAG, "found query=" + query);
                                    found(getString(R.string.schedule_room_set) + " \"" + json.getString("label") + "\"");
                                    break;
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
                    Log.w(TAG, "failed to find schedule");
                    query = null;
                    found(getString(R.string.schedule_not_found));
                }
            }
        });
    }

    @Override
    public void onNewHandle(RequestHandle requestHandle) {
        widgetRequestHandle = requestHandle;
    }

    private boolean setQuery() {
        Log.v(TAG, "setQuery");
        EditText slw_input = (EditText) findViewById(R.id.slw_input);
        if (slw_input != null) {
            query = null;
            String search = slw_input.getText().toString().trim();
            if (!Objects.equals(search, "")) {
                Log.v(TAG, "setQuery and search | " + search);
                scheduleLessons.search(search);
                slw_input.clearFocus();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void loading(final String text) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loading | " + text);
                FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
                if (slw_container != null){
                    LinearLayout linearLayout = new LinearLayout(self);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), (int) (10 * destiny));
                    ProgressBar progressBar = new ProgressBar(self);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.CENTER;
                    progressBar.setLayoutParams(lp);
                    linearLayout.addView(progressBar);
                    TextView textView = new TextView(self);
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
        });
    }
    private void found(final String text) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "found | " + text);
                FrameLayout slw_container = (FrameLayout) findViewById(R.id.slw_container);
                if (slw_container != null){
                    LinearLayout linearLayout = new LinearLayout(self);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), (int) (10 * destiny));
                    TextView textView = new TextView(self);
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
        });
    }

    private void updateTimeSummary() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView slw_update_time_summary = (TextView) findViewById(R.id.slw_update_time_summary);
                if (slw_update_time_summary != null) {
                    String summary;
                    switch (updateTime){
                        case 0: summary = getString(R.string.manually); break;
                        case 12: summary = getString(R.string.once_per_12_hours); break;
                        case 24: summary = getString(R.string.once_per_1_day); break;
                        case 168: summary = getString(R.string.once_per_1_week); break;
                        case 672: summary = getString(R.string.once_per_4_weeks); break;
                        default: summary = getString(R.string.unknown); break;
                    }
                    slw_update_time_summary.setText(summary);
                }
            }
        });
    }
    private void close(int result, Intent intent) {
        Log.v(TAG, "close | result=" + result);
        if (intent == null){
            setResult(result);
        } else {
            setResult(result, intent);
        }
        if (widgetRequestHandle != null) widgetRequestHandle.cancel(true);
        finish();
    }

    public static void savePref(Context context, int appWidgetId, String type, String text) {
        Log.v(TAG, "savePref | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "savePref | prevented due to invalid appwidget id");
            return;
        }
        Storage.file.general.put(context, "widget_schedule_lessons#" + appWidgetId + "#" + type, text);
    }
    public static void deletePref(Context context, int appWidgetId, String type) {
        Log.v(TAG, "deletePref | appWidgetId=" + appWidgetId + " | type=" + type);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "deletePref | prevented due to invalid appwidget id");
            return;
        }
        Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#" + type);
    }
    public static String getPref(Context context, int appWidgetId, String type) {
        Log.v(TAG, "getPref | appWidgetId=" + appWidgetId + " | type=" + type);
        String pref;
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "getPref | prevented due to invalid appwidget id");
            pref = "";
        } else {
            pref = Storage.file.general.get(context, "widget_schedule_lessons#" + appWidgetId + "#" + type).trim();
        }
        return pref.isEmpty() ? null : pref;
    }
    public static JSONObject getPrefJson(Context context, int appWidgetId, String type) {
        JSONObject pref;
        try {
            String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, type);
            if (tmp == null) throw new NullPointerException(type + " is null");
            pref = new JSONObject(tmp);
        } catch (Exception e) {
            pref = null;
        }
        return pref;
    }

}

