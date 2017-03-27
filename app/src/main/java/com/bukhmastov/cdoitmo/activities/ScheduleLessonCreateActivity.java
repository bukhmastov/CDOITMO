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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.objects.entities.LessonUnit;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonCreateActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleLessonCreate";
    private LessonUnit lessonUnit;
    private Activity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Static.darkTheme) setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
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
            ((TextView) findViewById(R.id.slc_title)).setText(extras.getString("title"));
            lessonUnit.cache_token = extras.getString("cache_token");
            lessonUnit.day = extras.getInt("day");
            lessonUnit.week = extras.getInt("week");
            if (extras.getString("group") != null) {
                lessonUnit.group = extras.getString("group");
                ((TextView) findViewById(R.id.lesson_group)).setText(lessonUnit.group);
            }
            if (extras.getString("teacher") != null) {
                lessonUnit.teacher = extras.getString("teacher");
                lessonUnit.teacher_id = extras.getString("teacher_id");
                TeacherSearch.blocked = true;
                ((TextView) findViewById(R.id.lesson_teacher)).setText(lessonUnit.teacher);
            }
            if (extras.getString("room") != null) {
                lessonUnit.room = extras.getString("room");
                ((TextView) findViewById(R.id.lesson_room)).setText(lessonUnit.room);
            }

            TextInputEditText lesson_title = (TextInputEditText) findViewById(R.id.lesson_title);
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

            ((TextInputEditText) findViewById(R.id.lesson_time_start)).addTextChangedListener(new TextWatcher() {
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

            ((TextInputEditText) findViewById(R.id.lesson_time_end)).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.timeEnd = s.toString();
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
                    String type = s.toString();
                    if (Objects.equals(type.toLowerCase(), getString(R.string.lecture).toLowerCase())) {
                        type = "lecture";
                    }
                    if (Objects.equals(type.toLowerCase(), getString(R.string.practice).toLowerCase())) {
                        type = "practice";
                    }
                    if (Objects.equals(type.toLowerCase(), getString(R.string.lab).toLowerCase())) {
                        type = "lab";
                    }
                    lessonUnit.type = type;
                }
            });

            ((TextInputEditText) findViewById(R.id.lesson_group)).addTextChangedListener(new TextWatcher() {
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
            teacherPickerAdapter.setNotifyOnChange(true);
            lesson_teacher.setThreshold(2);
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
                        TeacherSearch.search(getBaseContext(), query, new TeacherSearch.response() {
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
                        lessonUnit.teacher = item.getString("person");
                        lessonUnit.teacher_id = String.valueOf(item.getInt("pid"));
                        Log.d(TAG, lessonUnit.teacher + " | " + lessonUnit.teacher_id);
                        TeacherSearch.blocked = true;
                        lesson_teacher.setText(lessonUnit.teacher);
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(self, getString(R.string.something_went_wrong));
                    }
                }
            });

            ((TextInputEditText) findViewById(R.id.lesson_room)).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lessonUnit.room = s.toString();
                }
            });

            ((TextInputEditText) findViewById(R.id.lesson_building)).addTextChangedListener(new TextWatcher() {
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
                    if (lessonUnit.title == null || lessonUnit.title.isEmpty()) {
                        Static.snackBar(self, "Необходимо указать название предмета");
                        return;
                    }
                    if (lessonUnit.timeStart == null || lessonUnit.timeStart.isEmpty()) {
                        Static.snackBar(self, "Необходимо указать время начала занятия");
                        return;
                    }
                    if (ScheduleLessons.createLesson(self, lessonUnit)) {
                        ScheduleLessonsFragment.reScheduleRequired = true;
                        finish();
                    } else {
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

        interface response {
            void onProgress(int state);
            void onFailure(int state);
            void onSuccess(JSONObject json);
        }

        private static RequestHandle request = null;
        public static boolean blocked = false;

        public static void search(final Context context, final String query, final TeacherSearch.response delegate){
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
                    delegate.onProgress(state);
                }
                @Override
                public void onFailure(int state) {
                    delegate.onFailure(state);
                }
                @Override
                public void onSuccess(JSONObject json) {
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
