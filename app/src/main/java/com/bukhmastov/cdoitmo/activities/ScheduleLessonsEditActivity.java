package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.objects.entities.LessonUnit;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsEditActivity extends AppCompatActivity {

    private static final String TAG = "SLEditActivity";
    private LessonUnit lessonUnit;
    private Activity self;
    private String cache_token;
    private int index;
    private String oldHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Activity created");
        setContentView(R.layout.activity_schedule_lesson_create);
        self = this;
        try {
            setSupportActionBar((Toolbar) findViewById(R.id.toolbar_lesson_create));
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            lessonUnit = new LessonUnit();
            Bundle extras = getIntent().getExtras();

            lessonUnit.day = extras.getInt("day");
            lessonUnit.week = extras.getInt("week");
            if (extras.getString("header") != null) {
                ((TextView) findViewById(R.id.slc_title)).setText(extras.getString("header"));
                TextView slc_desc = (TextView) findViewById(R.id.slc_desc);
                if (slc_desc != null) {
                    if (Static.week >= 0) {
                        slc_desc.setText(Static.week + " " + getString(R.string.school_week));
                    } else {
                        slc_desc.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT).format(new Date(Calendar.getInstance().getTimeInMillis())));
                    }
                }
                ((Button) findViewById(R.id.lesson_create_button)).setText(R.string.save);
            } else throw new Exception("Missed header extra");
            if (extras.getString("cache_token") != null) {
                lessonUnit.cache_token = extras.getString("cache_token");
            } else throw new Exception("Missed cache_token extra");
            final String hash;
            if (extras.getString("hash") != null) {
                hash = extras.getString("hash");
            } else throw new Exception("Missed hash extra");

            cache_token = lessonUnit.cache_token;
            index = lessonUnit.day;
            oldHash = hash;

            String addedStr = Storage.file.perm.get(this, "schedule_lessons#added#" + lessonUnit.cache_token, "");
            JSONArray added;
            if (addedStr.isEmpty()) {
                added = new JSONArray();
            } else {
                added = new JSONArray(addedStr);
            }
            boolean found = false;
            for (int i = 0; i < added.length(); i++) {
                JSONObject day = added.getJSONObject(i);
                if (day.getInt("index") == lessonUnit.day) {
                    JSONArray lessons = day.getJSONArray("lessons");
                    for (int j = 0; j < lessons.length(); j++) {
                        JSONObject lesson = lessons.getJSONObject(j);
                        if (Objects.equals(hash, Static.crypt(lesson.toString()))) {
                            if (lesson.getString("subject") != null) lessonUnit.title = lesson.getString("subject");
                            if (lesson.getString("timeStart") != null) lessonUnit.timeStart = lesson.getString("timeStart");
                            if (lesson.getString("timeEnd") != null) lessonUnit.timeEnd = lesson.getString("timeEnd");
                            if (lesson.getString("type") != null) lessonUnit.type = lesson.getString("type");
                            if (lesson.getString("group") != null) lessonUnit.group = lesson.getString("group");
                            if (lesson.getString("teacher") != null) lessonUnit.teacher = lesson.getString("teacher");
                            if (lesson.getString("teacher_id") != null) lessonUnit.teacher_id = lesson.getString("teacher_id");
                            if (lesson.getString("room") != null) lessonUnit.room = lesson.getString("room");
                            if (lesson.getString("building") != null) lessonUnit.building = lesson.getString("building");
                            if (lessonUnit.type != null) {
                                switch (lessonUnit.type) {
                                    case "practice": lessonUnit.type = getString(R.string.practice); break;
                                    case "lecture": lessonUnit.type = getString(R.string.lecture); break;
                                    case "lab": lessonUnit.type = getString(R.string.lab); break;
                                }
                            }
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) throw new Exception("Custom lesson not found");

            TextInputEditText lesson_title = (TextInputEditText) findViewById(R.id.lesson_title);
            if (lessonUnit.title != null) lesson_title.setText(lessonUnit.title);
            lesson_title.requestFocus();
            lesson_title.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.title = s.toString();
                }
            });

            TextInputEditText lesson_time_start = (TextInputEditText) findViewById(R.id.lesson_time_start);
            if (lessonUnit.timeStart != null) lesson_time_start.setText(lessonUnit.timeStart);
            lesson_time_start.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.timeStart = s.toString();
                    TextInputEditText lesson_time_end = (TextInputEditText) findViewById(R.id.lesson_time_end);
                    if (lesson_time_end.getText().toString().isEmpty()) {
                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(s.toString());
                        if (time.find()) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                            calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                            calendar.set(Calendar.SECOND, 0);
                            long timestamp = calendar.getTimeInMillis();
                            calendar = Calendar.getInstance();
                            calendar.setTime(new Date(timestamp + 5400000));
                            lesson_time_end.setText(ldgZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + ldgZero(calendar.get(Calendar.MINUTE)));
                        }
                    }
                }
            });

            TextInputEditText lesson_time_end = (TextInputEditText) findViewById(R.id.lesson_time_end);
            if (lessonUnit.timeEnd != null) lesson_time_end.setText(lessonUnit.timeEnd);
            lesson_time_end.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.timeEnd = s.toString();
                    TextInputEditText lesson_time_start = (TextInputEditText) findViewById(R.id.lesson_time_start);
                    if (lesson_time_start.getText().toString().isEmpty()) {
                        Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(s.toString());
                        if (time.find()) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                            calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                            calendar.set(Calendar.SECOND, 0);
                            long timestamp = calendar.getTimeInMillis();
                            calendar = Calendar.getInstance();
                            calendar.setTime(new Date(timestamp - 5400000));
                            lesson_time_start.setText(ldgZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + ldgZero(calendar.get(Calendar.MINUTE)));
                        }
                    }
                }
            });

            Spinner lesson_week = (Spinner) findViewById(R.id.lesson_week);
            ArrayAdapter<?> lesson_week_adapter = ArrayAdapter.createFromResource(this, R.array.week_types_titles, R.layout.spinner_layout_simple);
            lesson_week_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            lesson_week.setAdapter(lesson_week_adapter);
            switch (lessonUnit.week) {
                case 2: lesson_week.setSelection(0); break;
                case 0: lesson_week.setSelection(1); break;
                case 1: lesson_week.setSelection(2); break;
            }
            lesson_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    String[] week_types_values = getResources().getStringArray(R.array.week_types_values);
                    lessonUnit.week = Integer.parseInt(week_types_values[position]);
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Spinner lesson_day_of_week = (Spinner) findViewById(R.id.lesson_day_of_week);
            ArrayAdapter<?> lesson_day_of_week_adapter = ArrayAdapter.createFromResource(this, R.array.days_of_week_titles, R.layout.spinner_layout_simple);
            lesson_day_of_week_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            lesson_day_of_week.setAdapter(lesson_day_of_week_adapter);
            lesson_day_of_week.setSelection(lessonUnit.day);
            lesson_day_of_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                    String[] week_types_values = getResources().getStringArray(R.array.days_of_week_values);
                    lessonUnit.day = Integer.parseInt(week_types_values[position]);
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            final AutoCompleteTextView lesson_type = (AutoCompleteTextView) findViewById(R.id.lesson_type);
            if (lessonUnit.type != null) lesson_type.setText(lessonUnit.type);
            String[] types = {getString(R.string.lecture), getString(R.string.practice), getString(R.string.lab)};
            lesson_type.setThreshold(1);
            lesson_type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types));
            lesson_type.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.type = s.toString();
                }
            });

            TextInputEditText lesson_group = (TextInputEditText) findViewById(R.id.lesson_group);
            if (lessonUnit.group != null) lesson_group.setText(lessonUnit.group);
            lesson_group.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.group = s.toString();
                }
            });

            final AutoCompleteTextView lesson_teacher = (AutoCompleteTextView) findViewById(R.id.lesson_teacher);
            final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(this, new ArrayList<JSONObject>());
            if (lessonUnit.teacher != null) {
                ScheduleLessonsEditActivity.TeacherSearch.blocked = true;
                lesson_teacher.setText(lessonUnit.teacher);
            }
            teacherPickerAdapter.setNotifyOnChange(true);
            lesson_teacher.setThreshold(1);
            lesson_teacher.setAdapter(teacherPickerAdapter);
            lesson_teacher.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().trim();
                    teacherPickerAdapter.clear();
                    lesson_teacher.dismissDropDown();
                    if (!query.isEmpty()) {
                        ScheduleLessonsEditActivity.TeacherSearch.search(getBaseContext(), query, new ScheduleLessonsEditActivity.TeacherSearch.response() {
                            @Override
                            public void onSuccess(JSONObject json) {
                                teacherPickerAdapter.clear();
                                try {
                                    if (Objects.equals(json.getString("type"), "teacher_picker")) {
                                        JSONArray jsonArray = json.getJSONArray("list");
                                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            arrayList.add(jsonArray.getJSONObject(i));
                                        }
                                        teacherPickerAdapter.addAll(arrayList);
                                        teacherPickerAdapter.addTeachers(arrayList);
                                        if (arrayList.size() > 0) {
                                            lesson_teacher.showDropDown();
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onProgress(int state) {
                                lessonUnit.teacher_id = null;
                            }
                            @Override
                            public void onFailure(int state) {}
                        });
                    }
                }
            });
            lesson_teacher.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        JSONObject item = teacherPickerAdapter.getItem(position);
                        if (item == null)  throw new Exception("Teacher item is null");
                        lessonUnit.teacher = item.getString("person");
                        lessonUnit.teacher_id = String.valueOf(item.getInt("pid"));
                        Log.d(TAG, lessonUnit.teacher + " | " + lessonUnit.teacher_id);
                        ScheduleLessonsEditActivity.TeacherSearch.blocked = true;
                        lesson_teacher.setText(lessonUnit.teacher);
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(self, getString(R.string.something_went_wrong));
                    }
                }
            });

            TextInputEditText lesson_room = (TextInputEditText) findViewById(R.id.lesson_room);
            if (lessonUnit.room != null) lesson_room.setText(lessonUnit.room);
            lesson_room.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.room = s.toString();
                }
            });

            AutoCompleteTextView lesson_building = (AutoCompleteTextView) findViewById(R.id.lesson_building);
            if (lessonUnit.building != null) lesson_building.setText(lessonUnit.building);
            lesson_building.setThreshold(1);
            lesson_building.setAdapter(ArrayAdapter.createFromResource(this, R.array.buildings, android.R.layout.simple_dropdown_item_1line));
            lesson_building.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.building = s.toString();
                }
            });

            findViewById(R.id.lesson_create_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "create_button clicked");
                    if (lessonUnit.title == null || lessonUnit.title.isEmpty()) {
                        Log.v(TAG, "lessonUnit.title required");
                        Static.snackBar(self, getString(R.string.lesson_title_required));
                        return;
                    }
                    if (lessonUnit.timeStart == null || lessonUnit.timeStart.isEmpty()) {
                        Log.v(TAG, "lessonUnit.timeStart required");
                        Static.snackBar(self, getString(R.string.lesson_time_start_required));
                        return;
                    }
                    if (lessonUnit.type != null) {
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.lecture).toLowerCase())) lessonUnit.type = "lecture";
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.practice).toLowerCase())) lessonUnit.type = "practice";
                        if (Objects.equals(lessonUnit.type.toLowerCase(), getString(R.string.lab).toLowerCase())) lessonUnit.type = "lab";
                    }
                    if (ScheduleLessons.editLesson(self, cache_token, index, oldHash, lessonUnit)) {
                        ScheduleLessonsFragment.reScheduleRequired = true;
                        finish();
                    } else {
                        Log.w(TAG, "failed to create lesson");
                        Static.snackBar(self, getString(R.string.something_went_wrong));
                    }
                }
            });

        } catch (Exception e) {
            Static.error(e);
            Static.snackBar(this, getString(R.string.something_went_wrong));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private String ldgZero(int time){
        return time % 10 == time ? "0" + String.valueOf(time) : String.valueOf(time);
    }

    private static class TeacherSearch {

        private static final String TAG = "SLEditActivity.TS";
        interface response {
            void onProgress(int state);
            void onFailure(int state);
            void onSuccess(JSONObject json);
        }

        private static RequestHandle request = null;
        static boolean blocked = false;

        public static void search(final Context context, final String query, final ScheduleLessonsEditActivity.TeacherSearch.response delegate){
            Log.v(TAG, "search | query=" + query);
            if (request != null) {
                request.cancel(true);
                request = null;
            }
            if (blocked) {
                blocked = false;
                return;
            }
            ScheduleLessons scheduleLessons = new ScheduleLessons(context);
            scheduleLessons.setHandler(new ScheduleLessons.response() {
                @Override
                public void onProgress(int state) {
                    Log.v(TAG, "search | progress " + state);
                    delegate.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    Log.v(TAG, "search | failure " + state);
                    delegate.onFailure(state);
                }
                @Override
                public void onSuccess(JSONObject json) {
                    Log.v(TAG, "search | success");
                    delegate.onSuccess(json);
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    request = requestHandle;
                }
            });
            scheduleLessons.search(query);
        }

    }

}
