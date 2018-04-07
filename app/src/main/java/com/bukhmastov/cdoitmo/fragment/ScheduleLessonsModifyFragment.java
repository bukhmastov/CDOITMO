package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsModifyFragment extends ConnectedFragment {

    private static final String TAG = "SLModifyFragment";
    private @TYPE String type;
    private boolean block_time_start = false;
    private boolean block_time_end = false;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CREATE, EDIT})
    public @interface TYPE {}
    public static final String CREATE = "create";
    public static final String EDIT = "edit";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_lessons_modify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            final Bundle extras = getArguments();
            if (extras == null) {
                throw new NullPointerException("extras cannot be null");
            }
            type = extras.getString("action_type");
            if (type == null) {
                throw new NullPointerException("type cannot be null");
            }
            switch (type) {
                case CREATE: activity.updateToolbar(activity, activity.getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons); break;
                case EDIT: activity.updateToolbar(activity, activity.getString(R.string.lesson_editing), R.drawable.ic_schedule_lessons); break;
                default:
                    Exception exception = new Exception("got wrong type from arguments bundle: " + type);
                    Log.wtf(exception);
                    throw exception;
            }
            display(extras);
        } catch (Exception e) {
            Static.error(e);
            close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    private void display(final Bundle extras) {
        Static.T.runThread(() -> {
            try {
                final int week = Static.getWeek(activity);
                final String query = getStringExtra(extras, "query", true);
                final String type_lesson = getStringExtra(extras, "type", true);
                final String title = getStringExtra(extras, "title", true);
                final int weekday = getIntExtra(extras, "weekday", true);
                final JSONObject lessonOriginal = Static.string2json(getStringExtra(extras, "lesson", true));
                final LessonUnit lesson = convertJson2LessonUnit(lessonOriginal);
                Static.T.runOnUiThread(() -> {
                    try {
                        if (lesson == null) {
                            throw new NullPointerException("lesson cannot be null");
                        }
                        lesson.weekday = weekday;
                        TextView slc_title = activity.findViewById(R.id.slc_title);
                        TextView slc_desc = activity.findViewById(R.id.slc_desc);
                        if (slc_title != null) {
                            slc_title.setText(ScheduleLessons.getScheduleHeader(activity, title, type_lesson));
                        }
                        if (slc_desc != null) {
                            slc_desc.setText(ScheduleLessons.getScheduleWeek(activity, week));
                        }
                        // ---------
                        TextInputEditText lesson_title = activity.findViewById(R.id.lesson_title);
                        if (lesson.subject != null) lesson_title.setText(lesson.subject);
                        lesson_title.requestFocus();
                        lesson_title.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                lesson.subject = s.toString();
                            }
                        });
                        // ---------
                        final TextInputEditText lesson_time_start = activity.findViewById(R.id.lesson_time_start);
                        final TextInputEditText lesson_time_end = activity.findViewById(R.id.lesson_time_end);
                        if (lesson.timeStart != null) lesson_time_start.setText(lesson.timeStart);
                        if (lesson.timeEnd != null) lesson_time_end.setText(lesson.timeEnd);
                        lesson_time_start.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                if (block_time_start) {
                                    block_time_start = false;
                                    return;
                                }
                                String st = s.toString().trim();
                                Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                                if (time.find()) {
                                    Calendar st_calendar = Static.getCalendar();
                                    st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                                    st_calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                                    st_calendar.set(Calendar.SECOND, 0);
                                    st = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(st_calendar.get(Calendar.MINUTE));
                                    if (lesson_time_end.getText().toString().isEmpty()) {
                                        Calendar nt_calendar = Static.getCalendar();
                                        nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                                        block_time_end = true;
                                        String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(nt_calendar.get(Calendar.MINUTE));
                                        lesson.timeEnd = insert;
                                        int selection = lesson_time_end.getSelectionStart();
                                        lesson_time_end.setText(insert);
                                        try {
                                            lesson_time_end.setSelection(selection);
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    } else {
                                        String nt = lesson_time_end.getText().toString();
                                        Matcher next_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(nt);
                                        if (next_time.find()) {
                                            Calendar nt_calendar = Static.getCalendar();
                                            nt_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(next_time.group(1)));
                                            nt_calendar.set(Calendar.MINUTE, Integer.parseInt(next_time.group(2)));
                                            nt_calendar.set(Calendar.SECOND, 0);
                                            if (nt_calendar.getTimeInMillis() <= st_calendar.getTimeInMillis()) {
                                                nt_calendar.setTime(new Date(st_calendar.getTimeInMillis() + 5400000));
                                                block_time_end = true;
                                                String insert = nt_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(nt_calendar.get(Calendar.MINUTE));
                                                lesson.timeEnd = insert;
                                                int selection = lesson_time_end.getSelectionStart();
                                                lesson_time_end.setText(insert);
                                                try {
                                                    lesson_time_end.setSelection(selection);
                                                } catch (Exception ignore) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    }
                                    if (!s.toString().trim().equals(st)) {
                                        block_time_start = true;
                                        int selection = lesson_time_start.getSelectionStart();
                                        lesson_time_start.setText(st);
                                        try {
                                            lesson_time_start.setSelection(selection);
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    }
                                }
                                lesson.timeStart = st;
                            }
                        });
                        lesson_time_end.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                if (block_time_end) {
                                    block_time_end = false;
                                    return;
                                }
                                String et = s.toString().trim();
                                Matcher time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(et);
                                if (time.find()) {
                                    Calendar et_calendar = Static.getCalendar();
                                    et_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.group(1)));
                                    et_calendar.set(Calendar.MINUTE, Integer.parseInt(time.group(2)));
                                    et_calendar.set(Calendar.SECOND, 0);
                                    et = et_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(et_calendar.get(Calendar.MINUTE));
                                    if (lesson_time_start.getText().toString().isEmpty()) {
                                        Calendar st_calendar = Static.getCalendar();
                                        st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                                        block_time_start = true;
                                        String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(st_calendar.get(Calendar.MINUTE));
                                        lesson.timeStart = insert;
                                        int selection = lesson_time_start.getSelectionStart();
                                        lesson_time_start.setText(insert);
                                        try {
                                            lesson_time_start.setSelection(selection);
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    } else {
                                        String st = lesson_time_start.getText().toString();
                                        Matcher previous_time = Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(st);
                                        if (previous_time.find()) {
                                            Calendar st_calendar = Static.getCalendar();
                                            st_calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(previous_time.group(1)));
                                            st_calendar.set(Calendar.MINUTE, Integer.parseInt(previous_time.group(2)));
                                            st_calendar.set(Calendar.SECOND, 0);
                                            if (st_calendar.getTimeInMillis() >= et_calendar.getTimeInMillis()) {
                                                st_calendar.setTime(new Date(et_calendar.getTimeInMillis() - 5400000));
                                                block_time_start = true;
                                                String insert = st_calendar.get(Calendar.HOUR_OF_DAY) + ":" + Static.ldgZero(st_calendar.get(Calendar.MINUTE));
                                                lesson.timeStart = insert;
                                                int selection = lesson_time_start.getSelectionStart();
                                                lesson_time_start.setText(insert);
                                                try {
                                                    lesson_time_start.setSelection(selection);
                                                } catch (Exception ignore) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    }
                                    if (!s.toString().trim().equals(et)) {
                                        block_time_end = true;
                                        int selection = lesson_time_end.getSelectionStart();
                                        lesson_time_end.setText(et);
                                        try {
                                            lesson_time_end.setSelection(selection);
                                        } catch (Exception ignore) {
                                            // ignore
                                        }
                                    }
                                }
                                lesson.timeEnd = et;
                            }
                        });
                        // ---------
                        Spinner lesson_week = activity.findViewById(R.id.lesson_week);
                        ArrayAdapter<?> lesson_week_adapter = ArrayAdapter.createFromResource(activity, R.array.week_types_titles, R.layout.spinner_layout_simple);
                        lesson_week_adapter.setDropDownViewResource(R.layout.spinner_layout);
                        lesson_week.setAdapter(lesson_week_adapter);
                        lesson_week.setSelection((lesson.week + 1) % 3);
                        lesson_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                String[] week_types_values = activity.getResources().getStringArray(R.array.week_types_values);
                                lesson.week = Integer.parseInt(week_types_values[position]);
                            }
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        // ---------
                        final String[] week_types_values = activity.getResources().getStringArray(R.array.days_of_week_values);
                        Spinner lesson_day_of_week = activity.findViewById(R.id.lesson_day_of_week);
                        ArrayAdapter<?> lesson_day_of_week_adapter = ArrayAdapter.createFromResource(activity, R.array.days_of_week_titles, R.layout.spinner_layout_simple);
                        lesson_day_of_week_adapter.setDropDownViewResource(R.layout.spinner_layout);
                        lesson_day_of_week.setAdapter(lesson_day_of_week_adapter);
                        lesson_day_of_week.setSelection(lesson.weekday < week_types_values.length ? lesson.weekday : 0);
                        lesson_day_of_week.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            public void onItemSelected(AdapterView<?> parent, View item, int position, long selectedId) {
                                lesson.weekday = Integer.parseInt(week_types_values[position]);
                            }
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        // ---------
                        final AutoCompleteTextView lesson_type = activity.findViewById(R.id.lesson_type);
                        if (lesson.type != null) lesson_type.setText(lesson.type);
                        lesson_type.setThreshold(1);
                        lesson_type.setAdapter(ArrayAdapter.createFromResource(activity, R.array.lessons_types, R.layout.spinner_layout_simple_expanded));
                        lesson_type.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                lesson.type = s.toString();
                            }
                        });
                        // ---------
                        TextInputEditText lesson_group = activity.findViewById(R.id.lesson_group);
                        if (lesson.group != null) lesson_group.setText(lesson.group);
                        lesson_group.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                lesson.group = s.toString();
                            }
                        });
                        // ---------
                        final AutoCompleteTextView lesson_teacher = activity.findViewById(R.id.lesson_teacher);
                        final ProgressBar lesson_teacher_bar = activity.findViewById(R.id.lesson_teacher_bar);
                        final TeacherPickerAdapter teacherPickerAdapter = new TeacherPickerAdapter(activity, new ArrayList<>());
                        if (lesson.teacher != null) {
                            TeacherSearch.blocked = true;
                            lesson_teacher.setText(lesson.teacher);
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
                                final String query1 = s.toString().trim();
                                teacherPickerAdapter.clear();
                                lesson_teacher.dismissDropDown();
                                if (!query1.isEmpty()) {
                                    TeacherSearch.search(activity, query1, lesson_teacher_bar, new TeacherSearch.response() {
                                        @Override
                                        public void onPermitted() {
                                            lesson.teacher = query1;
                                            lesson.teacher_id = "";
                                        }
                                        @Override
                                        public void onSuccess(final JSONObject json) {
                                            Static.T.runOnUiThread(() -> {
                                                try {
                                                    teacherPickerAdapter.clear();
                                                    if (json.getString("type").equals("teachers")) {
                                                        JSONArray schedule = json.getJSONArray("schedule");
                                                        ArrayList<JSONObject> arrayList = new ArrayList<>();
                                                        for (int i = 0; i < schedule.length(); i++) {
                                                            arrayList.add(schedule.getJSONObject(i));
                                                        }
                                                        teacherPickerAdapter.addAll(arrayList);
                                                        teacherPickerAdapter.addTeachers(arrayList);
                                                        if (arrayList.size() > 0) {
                                                            lesson_teacher.showDropDown();
                                                        }
                                                    }
                                                } catch (Exception ignore) {
                                                    // ignore
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                        lesson_teacher.setOnItemClickListener((parent, view, position, id) -> {
                            try {
                                JSONObject item = teacherPickerAdapter.getItem(position);
                                if (item == null) throw new Exception("Teacher item is null");
                                lesson.teacher = item.getString("person");
                                lesson.teacher_id = String.valueOf(item.getInt("pid"));
                                TeacherSearch.blocked = true;
                                TeacherSearch.lastQuery = lesson.teacher;
                                lesson_teacher.setText(lesson.teacher);
                                lesson_teacher.dismissDropDown();
                            } catch (Exception e) {
                                Static.error(e);
                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            }
                        });
                        // ---------
                        TextInputEditText lesson_room = activity.findViewById(R.id.lesson_room);
                        if (lesson.room != null) lesson_room.setText(lesson.room);
                        lesson_room.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                lesson.room = s.toString();
                            }
                        });
                        // ---------
                        AutoCompleteTextView lesson_building = activity.findViewById(R.id.lesson_building);
                        if (lesson.building != null) lesson_building.setText(lesson.building);
                        lesson_building.setThreshold(1);
                        lesson_building.setAdapter(ArrayAdapter.createFromResource(activity, R.array.buildings, R.layout.spinner_layout_simple_expanded));
                        lesson_building.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                lesson.building = s.toString();
                            }
                        });
                        // ---------
                        Button lesson_create_button = activity.findViewById(R.id.lesson_create_button);
                        switch (type) {
                            case CREATE: lesson_create_button.setText(activity.getString(R.string.create)); break;
                            case EDIT: lesson_create_button.setText(activity.getString(R.string.save)); break;
                        }
                        lesson_create_button.setOnClickListener(v -> Static.T.runThread(() -> {
                            try {
                                Log.v(TAG, "create_button clicked");
                                if (lesson.subject == null || lesson.subject.isEmpty()) {
                                    Log.v(TAG, "lessonUnit.title required");
                                    Static.snackBar(activity, activity.getString(R.string.lesson_title_required));
                                    return;
                                }
                                if (lesson.timeStart == null || lesson.timeStart.isEmpty()) {
                                    Log.v(TAG, "lessonUnit.timeStart required");
                                    Static.snackBar(activity, activity.getString(R.string.lesson_time_start_required));
                                    return;
                                }
                                if (lesson.type != null) {
                                    if (lesson.type.toLowerCase().equals(activity.getString(R.string.lecture).toLowerCase())) lesson.type = "lecture";
                                    if (lesson.type.toLowerCase().equals(activity.getString(R.string.practice).toLowerCase())) lesson.type = "practice";
                                    if (lesson.type.toLowerCase().equals(activity.getString(R.string.lab).toLowerCase())) lesson.type = "lab";
                                    if (lesson.type.toLowerCase().equals(activity.getString(R.string.iws).toLowerCase())) lesson.type = "iws";
                                }
                                switch (type) {
                                    case CREATE: {
                                        if (ScheduleLessons.createLesson(activity, query, lesson.weekday, convertLessonUnit2Json(lesson), null)) {
                                            ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                            close();
                                        } else {
                                            Log.w(TAG, "failed to create lesson");
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                        break;
                                    }
                                    case EDIT: {
                                        if (ScheduleLessons.deleteLesson(activity, query, weekday, lessonOriginal, null) && ScheduleLessons.createLesson(activity, query, lesson.weekday, convertLessonUnit2Json(lesson), null)) {
                                            ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                            close();
                                        } else {
                                            Log.w(TAG, "failed to create lesson");
                                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Static.error(e);
                                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                close();
                            }
                        }));
                    } catch (Exception e) {
                        Static.error(e);
                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        close();
                    }
                });
            } catch (Exception e) {
                Static.error(e);
                Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                close();
            }
        });
    }
    private LessonUnit convertJson2LessonUnit(JSONObject lesson) throws Exception {
        if (lesson == null) {
            throw new NullPointerException("lesson cannot be null");
        }
        LessonUnit lessonUnit = new LessonUnit();
        lessonUnit.subject = getString(lesson, "subject");
        lessonUnit.type = getString(lesson, "type");
        lessonUnit.week = getInt(lesson, "week");
        lessonUnit.timeStart = getString(lesson, "timeStart");
        lessonUnit.timeEnd = getString(lesson, "timeEnd");
        lessonUnit.group = getString(lesson, "group");
        lessonUnit.teacher = getString(lesson, "teacher");
        lessonUnit.teacher_id = getString(lesson, "teacher_id");
        lessonUnit.room = getString(lesson, "room");
        lessonUnit.building = getString(lesson, "building");
        lessonUnit.cdoitmo_type = "synthetic";
        switch (lessonUnit.type) {
            case "practice": lessonUnit.type = activity.getString(R.string.practice); break;
            case "lecture": lessonUnit.type = activity.getString(R.string.lecture); break;
            case "lab": lessonUnit.type = activity.getString(R.string.lab); break;
            case "iws": lessonUnit.type = activity.getString(R.string.iws); break;
        }
        return lessonUnit;
    }
    private JSONObject convertLessonUnit2Json(LessonUnit lessonUnit) throws Exception {
        if (lessonUnit == null) {
            throw new NullPointerException("lessonUnit cannot be null");
        }
        if (lessonUnit.type != null) {
            if (lessonUnit.type.toLowerCase().equals(activity.getString(R.string.lecture).toLowerCase())) lessonUnit.type = "lecture";
            if (lessonUnit.type.toLowerCase().equals(activity.getString(R.string.practice).toLowerCase())) lessonUnit.type = "practice";
            if (lessonUnit.type.toLowerCase().equals(activity.getString(R.string.lab).toLowerCase())) lessonUnit.type = "lab";
            if (lessonUnit.type.toLowerCase().equals(activity.getString(R.string.iws).toLowerCase())) lessonUnit.type = "iws";
        }
        JSONObject lesson = new JSONObject();
        lesson.put("subject", lessonUnit.subject);
        lesson.put("type", lessonUnit.type);
        lesson.put("week", lessonUnit.week);
        lesson.put("timeStart", lessonUnit.timeStart);
        lesson.put("timeEnd", lessonUnit.timeEnd);
        lesson.put("group", lessonUnit.group);
        lesson.put("teacher", lessonUnit.teacher);
        lesson.put("teacher_id", lessonUnit.teacher_id);
        lesson.put("room", lessonUnit.room);
        lesson.put("building", lessonUnit.building);
        lesson.put("cdoitmo_type", lessonUnit.cdoitmo_type);
        return lesson;
    }
    private String getStringExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            String value = extras.getString(key);
            if (value != null) {
                return value;
            } else {
                if (strict) {
                    throw new Exception("Missed extra for key: " + key);
                } else {
                    return "";
                }
            }
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return "";
            }
        }
    }
    private int getIntExtra(Bundle extras, String key, boolean strict) throws Exception {
        if (extras.containsKey(key)) {
            return extras.getInt(key);
        } else {
            if (strict) {
                throw new Exception("Missed extra for key: " + key);
            } else {
                return 0;
            }
        }
    }
    protected String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (json.isNull(key) || object == null) {
                return "";
            } else {
                try {
                    String value = (String) object;
                    return value.equals("null") ? "" : value;
                } catch (Exception e) {
                    return "";
                }
            }
        } else {
            return "";
        }
    }
    protected int getInt(JSONObject json, String key) {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static class LessonUnit {
        public String subject;
        public String type;
        public int week;
        public int weekday;
        public String timeStart;
        public String timeEnd;
        public String group;
        public String teacher;
        public String teacher_id;
        public String room;
        public String building;
        public String cdoitmo_type;
    }
    private static class TeacherSearch {

        private static final String TAG = "SLModifyFragment.TS";
        interface response {
            void onPermitted();
            void onSuccess(JSONObject json);
        }
        private static Client.Request requestHandle = null;
        static boolean blocked = false;
        private static String lastQuery = "";

        public static void search(final Context context, final String query, final ProgressBar progressBar, final response delegate) {
            Static.T.runThread(() -> {
                if (requestHandle != null) {
                    requestHandle.cancel();
                }
                boolean allowed = !lastQuery.equals(query);
                if (allowed) {
                    try {
                        new JSONObject(query);
                        allowed = false;
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                if (blocked || !allowed) {
                    blocked = false;
                    Static.T.runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }
                Log.v(TAG, "search | query=" + query);
                delegate.onPermitted();
                Static.T.runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
                new ScheduleLessons(new Schedule.Handler() {
                    @Override
                    public void onSuccess(JSONObject json, boolean fromCache) {
                        Log.v(TAG, "search | onSuccess");
                        Static.T.runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                        delegate.onSuccess(json);
                    }
                    @Override
                    public void onFailure(int state) {
                        this.onFailure(0, null, state);
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        Log.v(TAG, "search | onFailure | statusCode=" + statusCode + " | state=" + state);
                        Static.T.runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                    @Override
                    public void onProgress(int state) {
                        Log.v(TAG, "search | onProgress | state=" + state);
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
                }).search(context, query);
                lastQuery = query;
            });
        }
    }
}
